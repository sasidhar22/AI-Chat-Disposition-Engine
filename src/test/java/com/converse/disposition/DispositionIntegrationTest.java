package com.converse.disposition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.converse.disposition.client.BedrockClaudeClient;
import com.converse.disposition.client.CrmActivityClient;
import com.converse.disposition.model.*;
import com.converse.disposition.repository.DispositionRepository;
import com.converse.disposition.repository.MessageDocument;
import com.converse.disposition.service.DispositionCacheService;
import org.awaitility.Awaitility;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.converse.disposition.jooq.generated.tables.Agent.AGENT;
import static com.converse.disposition.jooq.generated.tables.ChatSession.CHAT_SESSION;
import static com.converse.disposition.jooq.generated.tables.Lead.LEAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class DispositionIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("converse_disposition")
            .withUsername("root")
            .withPassword("root");

    @Container
    static ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.13.0")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m");

    // Redis via test container
    @Container
    static com.redis.testcontainers.RedisContainer redis =
            new com.redis.testcontainers.RedisContainer(
                    com.redis.testcontainers.RedisContainer.DEFAULT_IMAGE_NAME
                            .withTag("7.2-alpine"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.elasticsearch.uris",  elasticsearch::getHttpHostAddress);
        registry.add("spring.data.redis.host",     redis::getHost);
        registry.add("spring.data.redis.port",     () -> redis.getMappedPort(6379));
    }

    @MockBean
    BedrockClaudeClient bedrockClaudeClient;

    @MockBean
    CrmActivityClient crmActivityClient;

    @Autowired MockMvc mockMvc;
    @Autowired DSLContext dsl;
    @Autowired DispositionRepository dispositionRepository;
    @Autowired DispositionCacheService cacheService;
    @Autowired ElasticsearchOperations elasticsearchOperations;
    @Autowired ObjectMapper objectMapper;

    private static final long SESSION_ID  = 19899288L;
    private static final String TENANT_ID = "test-tenant";
    private static final String LEAD_ID   = "lead-001";
    private static final String AGENT_ID  = "agent-001";

    @BeforeEach
    void setUp() {
        dsl.deleteFrom(com.converse.disposition.jooq.generated.tables.ChatDisposition.CHAT_DISPOSITION).execute();
        dsl.deleteFrom(CHAT_SESSION).execute();
        dsl.deleteFrom(LEAD).execute();
        dsl.deleteFrom(AGENT).execute();

        dsl.insertInto(LEAD)
                .set(LEAD.LEAD_ID, LEAD_ID)
                .set(LEAD.TENANT_ID, TENANT_ID)
                .set(LEAD.LEAD_NAME, "Rahul Sharma")
                .set(LEAD.LEAD_PHONE, "+91-9876543210")
                .execute();

        dsl.insertInto(AGENT)
                .set(AGENT.AGENT_ID, AGENT_ID)
                .set(AGENT.TENANT_ID, TENANT_ID)
                .set(AGENT.AGENT_NAME, "Priya Menon")
                .execute();

        dsl.insertInto(CHAT_SESSION)
                .set(CHAT_SESSION.SESSION_ID, SESSION_ID)
                .set(CHAT_SESSION.TENANT_ID, TENANT_ID)
                .set(CHAT_SESSION.LEAD_ID, LEAD_ID)
                .set(CHAT_SESSION.AGENT_ID, AGENT_ID)
                .set(CHAT_SESSION.CHANNEL, "WHATSAPP")
                .set(CHAT_SESSION.SESSION_STATUS, "ACTIVE")
                .set(CHAT_SESSION.TENANT_INDUSTRY, "BFSI")
                .set(CHAT_SESSION.SESSION_STARTED_AT,
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(15))
                .set(CHAT_SESSION.SESSION_ENDED_AT,
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                .execute();

        // Index a few transcript messages in Elasticsearch
        MessageDocument m1 = MessageDocument.builder()
                .messageId("msg-1")
                .sessionId(SESSION_ID)
                .participant("LEAD")
                .content("Hi I want to know about my home loan EMI")
                .sentAt(Instant.now().minusSeconds(900))
                .build();
        MessageDocument m2 = MessageDocument.builder()
                .messageId("msg-2")
                .sessionId(SESSION_ID)
                .participant("AGENT")
                .content("Could you share your registered mobile number?")
                .sentAt(Instant.now().minusSeconds(840))
                .build();
        elasticsearchOperations.save(m1);
        elasticsearchOperations.save(m2);
        elasticsearchOperations.indexOps(MessageDocument.class).refresh();
    }

    @Test
    void happyPath() throws Exception {
        DispositionSummary mockSummary = new DispositionSummary(
                "Lead enquired about home loan EMI. Agent requested registered mobile number.",
                "Lead wanted home loan EMI details.",
                "Agent asked for registered mobile number.",
                Sentiment.NEUTRAL,
                null,
                List.of()
        );
        when(bedrockClaudeClient.summarize(any(), any())).thenReturn(mockSummary);
        doNothing().when(crmActivityClient).postDisposition(any());

        mockMvc.perform(post("/internal/sessions/" + SESSION_ID + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closeReason\": \"AGENT_CLOSED\"}"))
                .andExpect(status().isAccepted());

        // Poll until disposition is COMPLETED (up to 10 seconds)
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            Optional<com.converse.disposition.jooq.generated.tables.records.ChatDispositionRecord> record =
                    dispositionRepository.findBySessionId(SESSION_ID);
            return record.isPresent() &&
                    (DispositionStatus.COMPLETED.name().equals(record.get().getStatus()) ||
                     DispositionStatus.FALLBACK.name().equals(record.get().getStatus()));
        });

        var record = dispositionRepository.findBySessionId(SESSION_ID);
        assertThat(record).isPresent();
        assertThat(record.get().getSummary()).isNotNull();
        assertThat(record.get().getSentiment()).isNotNull();

        // Verify Redis cached
        Optional<DispositionCard> cached = cacheService.get(TENANT_ID, SESSION_ID);
        assertThat(cached).isPresent();

        // Verify CRM was called
        verify(crmActivityClient, atLeastOnce()).postDisposition(any());
    }

    @Test
    void idempotency() throws Exception {
        DispositionSummary mockSummary = new DispositionSummary(
                "Summary text.",
                "Lead intent.",
                "Agent action.",
                Sentiment.POSITIVE,
                null,
                List.of()
        );
        when(bedrockClaudeClient.summarize(any(), any())).thenReturn(mockSummary);
        doNothing().when(crmActivityClient).postDisposition(any());

        // Fire close twice rapidly
        mockMvc.perform(post("/internal/sessions/" + SESSION_ID + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closeReason\": \"AGENT_CLOSED\"}"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/internal/sessions/" + SESSION_ID + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closeReason\": \"AGENT_CLOSED\"}"))
                .andExpect(status().isAccepted());

        // Wait for processing
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                dispositionRepository.findBySessionId(SESSION_ID)
                        .map(r -> DispositionStatus.COMPLETED.name().equals(r.getStatus()) ||
                                  DispositionStatus.FALLBACK.name().equals(r.getStatus()))
                        .orElse(false)
        );

        // Bedrock should only be called once
        verify(bedrockClaudeClient, times(1)).summarize(any(), any());
    }

    @Test
    void fallbackOnBedrockFailure() throws Exception {
        when(bedrockClaudeClient.summarize(any(), any()))
                .thenThrow(new com.converse.disposition.exception.BedrockInvocationException(
                        "Throttled"));
        doNothing().when(crmActivityClient).postDisposition(any());

        mockMvc.perform(post("/internal/sessions/" + SESSION_ID + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closeReason\": \"AGENT_CLOSED\"}"))
                .andExpect(status().isAccepted());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                dispositionRepository.findBySessionId(SESSION_ID)
                        .map(r -> DispositionStatus.FALLBACK.name().equals(r.getStatus()))
                        .orElse(false)
        );

        var record = dispositionRepository.findBySessionId(SESSION_ID);
        assertThat(record).isPresent();
        assertThat(record.get().getStatus()).isEqualTo(DispositionStatus.FALLBACK.name());
        assertThat(record.get().getSummary()).isNotNull();
    }
}
