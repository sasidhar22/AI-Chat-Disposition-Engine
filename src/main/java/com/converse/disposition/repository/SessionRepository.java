package com.converse.disposition.repository;

import com.converse.disposition.jooq.generated.tables.ChatSession;
import com.converse.disposition.jooq.generated.tables.records.ChatSessionRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SessionRepository {

    private static final ChatSession CS = ChatSession.CHAT_SESSION;

    private final DSLContext dsl;

    public SessionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<ChatSessionRecord> findById(long sessionId) {
        return dsl.selectFrom(CS)
                .where(CS.SESSION_ID.eq(sessionId))
                .fetchOptional();
    }
}
