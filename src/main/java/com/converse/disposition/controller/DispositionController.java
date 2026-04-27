package com.converse.disposition.controller;

import com.converse.disposition.jooq.generated.tables.records.ChatDispositionRecord;
import com.converse.disposition.model.*;
import com.converse.disposition.repository.DispositionRepository;
import com.converse.disposition.service.DispositionCacheService;
import com.converse.disposition.jooq.generated.tables.ChatDisposition;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/dispositions")
public class DispositionController {

    private final DispositionCacheService cacheService;
    private final DispositionRepository dispositionRepository;
    private final DSLContext dsl;

    public DispositionController(DispositionCacheService cacheService,
                                  DispositionRepository dispositionRepository,
                                  DSLContext dsl) {
        this.cacheService = cacheService;
        this.dispositionRepository = dispositionRepository;
        this.dsl = dsl;
    }

    @GetMapping("/by-session/{sessionId}")
    public ResponseEntity<?> getBySession(
            @PathVariable Long sessionId,
            @RequestParam String tenantId) {

        Optional<DispositionCard> cached = cacheService.get(tenantId, sessionId);
        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

        Optional<ChatDispositionRecord> recordOpt = dispositionRepository.findBySessionId(sessionId);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ChatDispositionRecord record = recordOpt.get();
        String status = record.getStatus();
        if (DispositionStatus.PENDING.name().equals(status) ||
                DispositionStatus.GENERATING.name().equals(status)) {
            return ResponseEntity.accepted().body(Map.of(
                    "message", "Disposition is being generated",
                    "status", status
            ));
        }

        DispositionCard card = toCard(record);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<List<Map<String, Object>>> getByLead(
            @PathVariable String leadId,
            @RequestParam String tenantId) {

        ChatDisposition CD = ChatDisposition.CHAT_DISPOSITION;
        List<Map<String, Object>> results = dsl.selectFrom(CD)
                .where(CD.LEAD_ID.eq(leadId).and(CD.TENANT_ID.eq(tenantId)))
                .orderBy(CD.CREATED_AT.desc())
                .limit(20)
                .fetch()
                .map(r -> {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("sessionId",       r.getSessionId());
                    row.put("sessionStartedAt", r.getSessionStartedAt());
                    row.put("sessionEndedAt",   r.getSessionEndedAt());
                    row.put("channel",          r.getChannel());
                    row.put("sentiment",        r.getSentiment());
                    row.put("status",           r.getStatus());
                    return row;
                });

        return ResponseEntity.ok(results);
    }

    private DispositionCard toCard(ChatDispositionRecord r) {
        Sentiment sentiment = null;
        try {
            if (r.getSentiment() != null) sentiment = Sentiment.valueOf(r.getSentiment());
        } catch (IllegalArgumentException ignored) {}

        DispositionSummary summary = new DispositionSummary(
                r.getSummary(),
                r.getLeadIntent(),
                r.getAgentAction(),
                sentiment,
                r.getUnresolved(),
                List.of()
        );

        Instant startedAt = r.getSessionStartedAt() != null
                ? r.getSessionStartedAt().toInstant(ZoneOffset.UTC) : Instant.now();
        Instant endedAt = r.getSessionEndedAt() != null
                ? r.getSessionEndedAt().toInstant(ZoneOffset.UTC) : Instant.now();

        Channel channel = Channel.WHATSAPP;
        try {
            if (r.getChannel() != null) channel = Channel.valueOf(r.getChannel());
        } catch (IllegalArgumentException ignored) {}

        boolean fallback = DispositionStatus.FALLBACK.name().equals(r.getStatus());

        return DispositionCard.builder()
                .sessionId(String.valueOf(r.getSessionId()))
                .tenantId(r.getTenantId())
                .leadId(r.getLeadId())
                .agentId(r.getAgentId())
                .leadName("Unknown Lead")
                .agentName("Unknown Agent")
                .channel(channel)
                .businessPhone(r.getBusinessPhone())
                .sessionStartedAt(startedAt)
                .sessionEndedAt(endedAt)
                .summary(summary)
                .generatedAt(Instant.now())
                .modelVersion(r.getModelVersion())
                .fallback(fallback)
                .build();
    }
}
