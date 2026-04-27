package com.converse.disposition.model;

import java.time.Instant;

public record SessionClosedEvent(
        long sessionId,
        String tenantId,
        String leadId,
        String agentId,
        Channel channel,
        String businessPhone,
        CloseReason closeReason,
        Instant sessionStartedAt,
        Instant sessionEndedAt,
        TenantIndustry tenantIndustry
) {}
