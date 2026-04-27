package com.converse.disposition.repository;

import com.converse.disposition.jooq.generated.tables.Agent;
import com.converse.disposition.jooq.generated.tables.records.AgentRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AgentRepository {

    private static final Agent A = Agent.AGENT;

    private final DSLContext dsl;

    public AgentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<AgentRecord> findById(String agentId) {
        return dsl.selectFrom(A)
                .where(A.AGENT_ID.eq(agentId))
                .fetchOptional();
    }
}
