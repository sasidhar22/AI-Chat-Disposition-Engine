package com.converse.disposition.jooq.generated.tables;

import com.converse.disposition.jooq.generated.tables.records.LeadRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import java.time.LocalDateTime;

@SuppressWarnings({"all","unchecked","rawtypes"})
public class Lead extends TableImpl<LeadRecord> {
    private static final long serialVersionUID = 1L;
    public static final Lead LEAD = new Lead();

    public final TableField<LeadRecord, String> LEAD_ID = createField(DSL.name("lead_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<LeadRecord, String> TENANT_ID = createField(DSL.name("tenant_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<LeadRecord, String> LEAD_NAME = createField(DSL.name("lead_name"), SQLDataType.VARCHAR(255).nullable(false), this, "");
    public final TableField<LeadRecord, String> LEAD_PHONE = createField(DSL.name("lead_phone"), SQLDataType.VARCHAR(24).nullable(true), this, "");
    public final TableField<LeadRecord, String> EMAIL = createField(DSL.name("email"), SQLDataType.VARCHAR(255).nullable(true), this, "");
    public final TableField<LeadRecord, LocalDateTime> CREATED_AT = createField(DSL.name("created_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");
    public final TableField<LeadRecord, LocalDateTime> UPDATED_AT = createField(DSL.name("updated_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");

    private Lead() { this(DSL.name("lead"), null); }
    private Lead(Name alias, Table<LeadRecord> aliased) { super(alias, null, aliased, null, DSL.comment(""), TableOptions.table()); }

    @Override public Class<LeadRecord> getRecordType() { return LeadRecord.class; }
    @Override public Lead as(String alias) { return new Lead(DSL.name(alias), this); }
    @Override public Lead as(Name alias) { return new Lead(alias, this); }
    @Override public UniqueKey<LeadRecord> getPrimaryKey() { return null; }
}
