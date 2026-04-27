package com.converse.disposition.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.converse.disposition.exception.BedrockInvocationException;
import com.converse.disposition.model.DispositionSummary;
import com.converse.disposition.model.SessionClosedEvent;
import com.converse.disposition.model.TranscriptMessage;
import com.converse.disposition.prompt.PromptBuilder;
import com.converse.disposition.service.DispositionMetrics;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BedrockClaudeClient {

    private static final int MAX_TOKENS = 1024;

    private final BedrockRuntimeClient bedrockClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final DispositionMetrics metrics;

    @Value("${aws.bedrock.model-id:anthropic.claude-3-5-sonnet-20241022-v2:0}")
    private String modelId;

    public BedrockClaudeClient(BedrockRuntimeClient bedrockClient,
                                PromptBuilder promptBuilder,
                                ObjectMapper objectMapper,
                                DispositionMetrics metrics) {
        this.bedrockClient = bedrockClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Retry(name = "bedrock")
    @CircuitBreaker(name = "bedrock")
    public DispositionSummary summarize(SessionClosedEvent event, List<TranscriptMessage> transcript) {
        String systemPrompt = promptBuilder.buildSystemPrompt(event);
        String userMessage  = promptBuilder.buildUserMessage(event, transcript);
        String rawJson = invokeModel(systemPrompt, userMessage);
        try {
            DispositionSummary summary = objectMapper.readValue(stripCodeFences(rawJson), DispositionSummary.class);
            metrics.recordBedrockCall("success");
            return summary;
        } catch (Exception e) {
            metrics.recordBedrockCall("failed");
            throw new BedrockInvocationException("Failed to parse Bedrock response as DispositionSummary", e);
        }
    }

    @Retry(name = "bedrock")
    public DispositionSummary repair(SessionClosedEvent event, List<TranscriptMessage> transcript,
                                     String rawJson, String validationError) {
        String systemPrompt = promptBuilder.buildSystemPrompt(event);
        String repairPrompt = promptBuilder.buildRepairPrompt(rawJson, validationError);
        String repairedJson = invokeModel(systemPrompt, repairPrompt);
        try {
            DispositionSummary summary = objectMapper.readValue(stripCodeFences(repairedJson), DispositionSummary.class);
            metrics.recordBedrockCall("retry");
            return summary;
        } catch (Exception e) {
            metrics.recordBedrockCall("failed");
            throw new BedrockInvocationException("Repair attempt also failed to produce valid JSON", e);
        }
    }

    private String invokeModel(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "anthropic_version", "bedrock-2023-05-31",
                    "max_tokens", MAX_TOKENS,
                    "system", systemPrompt,
                    "messages", List.of(
                            Map.of("role", "user", "content", userMessage)
                    )
            );
            String bodyJson = objectMapper.writeValueAsString(requestBody);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(bodyJson))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("content").get(0).path("text").asText();
        } catch (BedrockInvocationException e) {
            throw e;
        } catch (Exception e) {
            throw new BedrockInvocationException("Bedrock invocation failed", e);
        }
    }

    private String stripCodeFences(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end   = trimmed.lastIndexOf("```");
            if (start >= 0 && end > start) {
                return trimmed.substring(start + 1, end).trim();
            }
        }
        return trimmed;
    }
}
