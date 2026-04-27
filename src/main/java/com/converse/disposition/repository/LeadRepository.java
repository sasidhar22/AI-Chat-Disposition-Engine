package com.converse.disposition.repository;

import com.converse.disposition.jooq.generated.tables.Lead;
import com.converse.disposition.jooq.generated.tables.records.LeadRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LeadRepository {

    private static final Lead L = Lead.LEAD;

    private final DSLContext dsl;

    public LeadRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<LeadRecord> findById(String leadId) {
        return dsl.selectFrom(L)
                .where(L.LEAD_ID.eq(leadId))
                .fetchOptional();
    }
}
