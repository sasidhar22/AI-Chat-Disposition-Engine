package com.converse.disposition.jooq.generated.tables;

import com.converse.disposition.jooq.generated.tables.records.AgentRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import java.time.LocalDateTime;

@SuppressWarnings({"all","unchecked","rawtypes"})
public class Agent extends TableImpl<AgentRecord> {
    private static final long serialVersionUID = 1L;
    public static final Agent AGENT = new Agent();

    public final TableField<AgentRecord, String> AGENT_ID = createField(DSL.name("agent_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<AgentRecord, String> TENANT_ID = createField(DSL.name("tenant_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<AgentRecord, String> AGENT_NAME = createField(DSL.name("agent_name"), SQLDataType.VARCHAR(255).nullable(false), this, "");
    public final TableField<AgentRecord, String> EMAIL = createField(DSL.name("email"), SQLDataType.VARCHAR(255).nullable(true), this, "");
    public final TableField<AgentRecord, LocalDateTime> CREATED_AT = createField(DSL.name("created_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");
    public final TableField<AgentRecord, LocalDateTime> UPDATED_AT = createField(DSL.name("updated_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");

    private Agent() { this(DSL.name("agent"), null); }
    private Agent(Name alias, Table<AgentRecord> aliased) { super(alias, null, aliased, null, DSL.comment(""), TableOptions.table()); }

    @Override public Class<AgentRecord> getRecordType() { return AgentRecord.class; }
    @Override public Agent as(String alias) { return new Agent(DSL.name(alias), this); }
    @Override public Agent as(Name alias) { return new Agent(alias, this); }
    @Override public UniqueKey<AgentRecord> getPrimaryKey() { return null; }
}
