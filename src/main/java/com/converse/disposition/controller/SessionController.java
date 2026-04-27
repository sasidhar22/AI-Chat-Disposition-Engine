package com.converse.disposition.controller;

import com.converse.disposition.model.*;
import com.converse.disposition.repository.SessionRepository;
import com.converse.disposition.jooq.generated.tables.records.ChatSessionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/internal/sessions")
public class SessionController {

    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SessionController(SessionRepository sessionRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/{sessionId}/close")
    public ResponseEntity<Map<String, Object>> closeSession(
            @PathVariable long sessionId,
            @RequestBody CloseRequest request) {

        Optional<ChatSessionRecord> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ChatSessionRecord session = sessionOpt.get();
        String status = session.getSessionStatus();
        if ("CLOSED".equals(status) || "EXPIRED".equals(status)) {
            return ResponseEntity.status(409)
                    .body(Map.of("message", "Session already closed", "sessionId", sessionId));
        }

        TenantIndustry industry = TenantIndustry.OTHER;
        try {
            if (session.getTenantIndustry() != null) {
                industry = TenantIndustry.valueOf(session.getTenantIndustry());
            }
        } catch (IllegalArgumentException ignored) {}

        CloseReason closeReason = CloseReason.AGENT_CLOSED;
        try {
            if (request.closeReason() != null) {
                closeReason = CloseReason.valueOf(request.closeReason());
            }
        } catch (IllegalArgumentException ignored) {}

        SessionClosedEvent event = new SessionClosedEvent(
                sessionId,
                session.getTenantId(),
                session.getLeadId(),
                session.getAgentId(),
                Channel.valueOf(session.getChannel()),
                session.getBusinessPhone(),
                closeReason,
                session.getSessionStartedAt().toInstant(ZoneOffset.UTC),
                session.getSessionEndedAt() != null
                        ? session.getSessionEndedAt().toInstant(ZoneOffset.UTC)
                        : java.time.Instant.now(),
                industry
        );

        eventPublisher.publishEvent(event);
        log.info("Published SessionClosedEvent for sessionId={}", sessionId);

        return ResponseEntity.accepted().body(Map.of(
                "message", "Disposition generation started",
                "sessionId", sessionId
        ));
    }

    record CloseRequest(String closeReason) {}
}
