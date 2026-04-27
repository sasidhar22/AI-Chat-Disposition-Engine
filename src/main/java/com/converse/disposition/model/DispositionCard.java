package com.converse.disposition.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.Instant;

@Builder
public record DispositionCard(
        String sessionId,
        String tenantId,
        String leadId,
        String agentId,
        String leadName,
        String leadPhone,
        String agentName,
        Channel channel,
        String businessPhone,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant sessionStartedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant sessionEndedAt,
        DispositionSummary summary,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant generatedAt,
        String modelVersion,
        boolean fallback
) {}
