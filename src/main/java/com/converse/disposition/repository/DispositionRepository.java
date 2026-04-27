package com.converse.disposition.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.converse.disposition.jooq.generated.tables.ChatDisposition;
import com.converse.disposition.jooq.generated.tables.records.ChatDispositionRecord;
import com.converse.disposition.model.DispositionStatus;
import com.converse.disposition.model.DispositionSummary;
import com.converse.disposition.model.SessionClosedEvent;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.now;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.val;

@Repository
public class DispositionRepository {

    private static final ChatDisposition CD = ChatDisposition.CHAT_DISPOSITION;

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    @Value("${aws.bedrock.model-id:anthropic.claude-3-5-sonnet-20241022-v2:0}")
    private String modelId;

    public DispositionRepository(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    public void insertPending(SessionClosedEvent event) {
        try {
            dsl.insertInto(CD)
                    .set(CD.SESSION_ID, event.sessionId())
                    .set(CD.TENANT_ID, event.tenantId())
                    .set(CD.LEAD_ID, event.leadId())
                    .set(CD.AGENT_ID, event.agentId())
                    .set(CD.CHANNEL, event.channel().name())
                    .set(CD.BUSINESS_PHONE, event.businessPhone())
                    .set(CD.SESSION_STARTED_AT,
                            LocalDateTime.ofInstant(event.sessionStartedAt(), ZoneOffset.UTC))
                    .set(CD.SESSION_ENDED_AT,
                            LocalDateTime.ofInstant(event.sessionEndedAt(), ZoneOffset.UTC))
                    .set(CD.STATUS, DispositionStatus.PENDING.name())
                    .set(CD.ATTEMPT_COUNT, 0)
                    .execute();
        } catch (org.jooq.exception.DataAccessException ex) {
            if (ex.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException) {
                throw new DuplicateKeyException("Duplicate session_id: " + event.sessionId(), ex);
            }
            throw ex;
        }
    }

    public void updateStatus(long sessionId, DispositionStatus status) {
        dsl.update(CD)
                .set(CD.STATUS, status.name())
                .set(CD.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
                .where(CD.SESSION_ID.eq(sessionId))
                .execute();
    }

    public void markCompleted(long sessionId, DispositionSummary summary) {
        dsl.update(CD)
                .set(CD.STATUS, DispositionStatus.COMPLETED.name())
                .set(CD.SUMMARY, summary.summary())
                .set(CD.LEAD_INTENT, summary.leadIntent())
                .set(CD.AGENT_ACTION, summary.agentAction())
                .set(CD.SENTIMENT, summary.sentiment() != null ? summary.sentiment().name() : null)
                .set(CD.UNRESOLVED, summary.unresolved())
                .set(CD.KEY_ENTITIES_JSON, toJson(summary.keyEntities()))
                .set(CD.MODEL_VERSION, modelId)
                .set(CD.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
                .where(CD.SESSION_ID.eq(sessionId))
                .execute();
    }

    public void markFallback(long sessionId, DispositionSummary summary, String reason) {
        dsl.update(CD)
                .set(CD.STATUS, DispositionStatus.FALLBACK.name())
                .set(CD.SUMMARY, summary.summary())
                .set(CD.LEAD_INTENT, summary.leadIntent())
                .set(CD.AGENT_ACTION, summary.agentAction())
                .set(CD.SENTIMENT, summary.sentiment() != null ? summary.sentiment().name() : null)
                .set(CD.UNRESOLVED, summary.unresolved())
                .set(CD.KEY_ENTITIES_JSON, toJson(summary.keyEntities()))
                .set(CD.MODEL_VERSION, modelId)
                .set(CD.FAILURE_REASON, reason)
                .set(CD.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
                .where(CD.SESSION_ID.eq(sessionId))
                .execute();
    }

    public void markFailed(long sessionId, String reason) {
        dsl.update(CD)
                .set(CD.STATUS, DispositionStatus.FAILED.name())
                .set(CD.FAILURE_REASON, reason)
                .set(CD.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
                .where(CD.SESSION_ID.eq(sessionId))
                .execute();
    }

    public void markConverseDelivered(long sessionId) {
        dsl.update(CD)
                .set(CD.CONVERSE_DELIVERED_AT, LocalDateTime.now(ZoneOffset.UTC))
                .set(CD.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
                .where(CD.SESSION_ID.eq(sessionId))
                .execute();
    }

    public void markCrmDelivered(long sessionId) {
        dsl.update(CD)
                .set(CD.CRM_DELIVERED_AT, LocalDateTime.now(ZoneOffset.UTC))
                .set(CD.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
                .where(CD.SESSION_ID.eq(sessionId))
                .execute();
    }

    public Optional<ChatDispositionRecord> findBySessionId(long sessionId) {
        return dsl.selectFrom(CD)
                .where(CD.SESSION_ID.eq(sessionId))
                .fetchOptional();
    }

    public List<ChatDispositionRecord> findUndelivered(int ageMinutes) {
        return dsl.selectFrom(CD)
                .where(CD.STATUS.in(DispositionStatus.COMPLETED.name(), DispositionStatus.FALLBACK.name()))
                .and(CD.CRM_DELIVERED_AT.isNull().or(CD.CONVERSE_DELIVERED_AT.isNull()))
                .and(CD.CREATED_AT.lessThan(
                        field("NOW() - INTERVAL " + ageMinutes + " MINUTE", LocalDateTime.class)))
                .limit(100)
                .fetch();
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
