package com.converse.disposition.service;

import com.converse.disposition.model.*;
import com.converse.disposition.repository.AgentRepository;
import com.converse.disposition.repository.LeadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DispositionAssembler {

    private final LeadRepository leadRepository;
    private final AgentRepository agentRepository;

    @Value("${aws.bedrock.model-id:anthropic.claude-3-5-sonnet-20241022-v2:0}")
    private String modelId;

    public DispositionAssembler(LeadRepository leadRepository, AgentRepository agentRepository) {
        this.leadRepository = leadRepository;
        this.agentRepository = agentRepository;
    }

    public DispositionCard assemble(SessionClosedEvent event, DispositionSummary summary, boolean fallback) {
        var lead  = leadRepository.findById(event.leadId());
        var agent = agentRepository.findById(event.agentId());

        String leadName  = lead.map(r -> r.getLeadName()).orElse("Unknown Lead");
        String leadPhone = lead.map(r -> r.getLeadPhone()).orElse(null);
        String agentName = agent.map(r -> r.getAgentName()).orElse("Unknown Agent");

        return DispositionCard.builder()
                .sessionId(String.valueOf(event.sessionId()))
                .tenantId(event.tenantId())
                .leadId(event.leadId())
                .agentId(event.agentId())
                .leadName(leadName)
                .leadPhone(leadPhone)
                .agentName(agentName)
                .channel(event.channel())
                .businessPhone(event.businessPhone())
                .sessionStartedAt(event.sessionStartedAt())
                .sessionEndedAt(event.sessionEndedAt())
                .summary(summary)
                .generatedAt(Instant.now())
                .modelVersion(modelId)
                .fallback(fallback)
                .build();
    }

    public DispositionCard assembleFallback(SessionClosedEvent event, String failureReason) {
        var lead  = leadRepository.findById(event.leadId());
        var agent = agentRepository.findById(event.agentId());

        String leadName  = lead.map(r -> r.getLeadName()).orElse("Unknown Lead");
        String leadPhone = lead.map(r -> r.getLeadPhone()).orElse(null);
        String agentName = agent.map(r -> r.getAgentName()).orElse("Unknown Agent");

        DispositionSummary ruleBased = new DispositionSummary(
                "Automated summary unavailable. Please review the transcript.",
                "Unknown — transcript unavailable at generation time",
                "Unknown",
                Sentiment.NEUTRAL,
                "Manual review required",
                List.of()
        );

        return DispositionCard.builder()
                .sessionId(String.valueOf(event.sessionId()))
                .tenantId(event.tenantId())
                .leadId(event.leadId())
                .agentId(event.agentId())
                .leadName(leadName)
                .leadPhone(leadPhone)
                .agentName(agentName)
                .channel(event.channel())
                .businessPhone(event.businessPhone())
                .sessionStartedAt(event.sessionStartedAt())
                .sessionEndedAt(event.sessionEndedAt())
                .summary(ruleBased)
                .generatedAt(Instant.now())
                .modelVersion(modelId)
                .fallback(true)
                .build();
    }
}
