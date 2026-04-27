package com.converse.disposition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.converse.disposition.client.BedrockClaudeClient;
import com.converse.disposition.exception.DispositionValidationException;
import com.converse.disposition.model.DispositionSummary;
import com.converse.disposition.model.SessionClosedEvent;
import com.converse.disposition.model.TranscriptMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DispositionValidator {

    private final BedrockClaudeClient bedrockClient;
    private final ObjectMapper objectMapper;

    public DispositionValidator(BedrockClaudeClient bedrockClient, ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
        this.objectMapper = objectMapper;
    }

    public DispositionSummary validateOrRepair(DispositionSummary summary,
                                               SessionClosedEvent event,
                                               List<TranscriptMessage> transcript) {
        List<String> errors = validate(summary);
        if (errors.isEmpty()) return summary;

        log.warn("Disposition validation failed for sessionId={}, errors={}", event.sessionId(), errors);
        String errorMessage = String.join("; ", errors);
        String rawJson = toJson(summary);

        DispositionSummary repaired = bedrockClient.repair(event, transcript, rawJson, errorMessage);

        List<String> repairErrors = validate(repaired);
        if (!repairErrors.isEmpty()) {
            throw new DispositionValidationException(
                    "Repair attempt also failed validation: " + String.join("; ", repairErrors));
        }
        return repaired;
    }

    private List<String> validate(DispositionSummary summary) {
        List<String> errors = new ArrayList<>();

        if (summary.summary() == null || summary.summary().isBlank()) {
            errors.add("summary is null or blank");
        } else if (summary.summary().length() > 800) {
            errors.add("summary exceeds 800 characters");
        }

        if (summary.sentiment() == null) {
            errors.add("sentiment is null");
        }

        if (summary.leadIntent() == null || summary.leadIntent().isBlank()) {
            errors.add("lead_intent is null or blank");
        }

        if (summary.agentAction() == null || summary.agentAction().isBlank()) {
            errors.add("agent_action is null or blank");
        }

        if (summary.keyEntities() == null) {
            errors.add("key_entities is null");
        }

        return errors;
    }

    private String toJson(DispositionSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }
}
