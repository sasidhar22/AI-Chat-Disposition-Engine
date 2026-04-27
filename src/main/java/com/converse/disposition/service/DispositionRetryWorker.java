package com.converse.disposition.service;

import com.converse.disposition.client.CrmActivityClient;
import com.converse.disposition.jooq.generated.tables.records.ChatDispositionRecord;
import com.converse.disposition.model.*;
import com.converse.disposition.repository.AgentRepository;
import com.converse.disposition.repository.DispositionRepository;
import com.converse.disposition.repository.LeadRepository;
import com.converse.disposition.websocket.DispositionWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
public class DispositionRetryWorker {

    private final DispositionRepository dispositionRepository;
    private final DispositionCacheService cacheService;
    private final DispositionWebSocketHandler webSocketHandler;
    private final CrmActivityClient crmClient;
    private final LeadRepository leadRepository;
    private final AgentRepository agentRepository;

    @Value("${aws.bedrock.model-id:anthropic.claude-3-5-sonnet-20241022-v2:0}")
    private String modelId;

    public DispositionRetryWorker(DispositionRepository dispositionRepository,
                                   DispositionCacheService cacheService,
                                   DispositionWebSocketHandler webSocketHandler,
                                   CrmActivityClient crmClient,
                                   LeadRepository leadRepository,
                                   AgentRepository agentRepository) {
        this.dispositionRepository = dispositionRepository;
        this.cacheService          = cacheService;
        this.webSocketHandler      = webSocketHandler;
        this.crmClient             = crmClient;
        this.leadRepository        = leadRepository;
        this.agentRepository       = agentRepository;
    }

    @Scheduled(fixedDelay = 300_000)
    public void retryUndelivered() {
        List<ChatDispositionRecord> records = dispositionRepository.findUndelivered(15);
        int processed = 0;

        for (ChatDispositionRecord record : records) {
            try {
                DispositionCard card = reconstructCard(record);
                cacheService.put(card);

                if (record.getCrmDeliveredAt() == null) {
                    try {
                        crmClient.postDisposition(card);
                        dispositionRepository.markCrmDelivered(record.getSessionId());
                    } catch (Exception e) {
                        log.warn("Retry CRM delivery failed for sessionId={}", record.getSessionId(), e);
                    }
                }

                if (record.getConverseDeliveredAt() == null) {
                    boolean pushed = webSocketHandler.pushToAgent(record.getAgentId(), card);
                    if (pushed) {
                        dispositionRepository.markConverseDelivered(record.getSessionId());
                    }
                }

                processed++;
            } catch (Exception e) {
                log.warn("Retry worker failed for sessionId={}", record.getSessionId(), e);
            }
        }

        log.info("Retry worker processed {} records", processed);
    }

    private DispositionCard reconstructCard(ChatDispositionRecord r) {
        String leadName  = leadRepository.findById(r.getLeadId())
                .map(l -> l.getLeadName()).orElse("Unknown Lead");
        String leadPhone = leadRepository.findById(r.getLeadId())
                .map(l -> l.getLeadPhone()).orElse(null);
        String agentName = agentRepository.findById(r.getAgentId())
                .map(a -> a.getAgentName()).orElse("Unknown Agent");

        DispositionSummary summary = new DispositionSummary(
                r.getSummary(),
                r.getLeadIntent(),
                r.getAgentAction(),
                r.getSentiment() != null ? Sentiment.valueOf(r.getSentiment()) : Sentiment.NEUTRAL,
                r.getUnresolved(),
                List.of()
        );

        return DispositionCard.builder()
                .sessionId(String.valueOf(r.getSessionId()))
                .tenantId(r.getTenantId())
                .leadId(r.getLeadId())
                .agentId(r.getAgentId())
                .leadName(leadName)
                .leadPhone(leadPhone)
                .agentName(agentName)
                .channel(Channel.valueOf(r.getChannel()))
                .businessPhone(r.getBusinessPhone())
                .sessionStartedAt(r.getSessionStartedAt().toInstant(ZoneOffset.UTC))
                .sessionEndedAt(r.getSessionEndedAt().toInstant(ZoneOffset.UTC))
                .summary(summary)
                .generatedAt(Instant.now())
                .modelVersion(r.getModelVersion() != null ? r.getModelVersion() : modelId)
                .fallback(DispositionStatus.FALLBACK.name().equals(r.getStatus()))
                .build();
    }
}
