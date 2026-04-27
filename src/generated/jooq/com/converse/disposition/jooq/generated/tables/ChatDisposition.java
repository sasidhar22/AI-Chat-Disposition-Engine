package com.converse.disposition.jooq.generated.tables;

import com.converse.disposition.jooq.generated.tables.records.ChatDispositionRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import java.time.LocalDateTime;

@SuppressWarnings({"all","unchecked","rawtypes"})
public class ChatDisposition extends TableImpl<ChatDispositionRecord> {
    private static final long serialVersionUID = 1L;
    public static final ChatDisposition CHAT_DISPOSITION = new ChatDisposition();

    public final TableField<ChatDispositionRecord, Long> ID = createField(DSL.name("id"), SQLDataType.BIGINT.nullable(false), this, "");
    public final TableField<ChatDispositionRecord, Long> SESSION_ID = createField(DSL.name("session_id"), SQLDataType.BIGINT.nullable(false), this, "");
    public final TableField<ChatDispositionRecord, String> TENANT_ID = createField(DSL.name("tenant_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<ChatDispositionRecord, String> LEAD_ID = createField(DSL.name("lead_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<ChatDispositionRecord, String> AGENT_ID = createField(DSL.name("agent_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<ChatDispositionRecord, String> CHANNEL = createField(DSL.name("channel"), SQLDataType.VARCHAR(24).nullable(false), this, "");
    public final TableField<ChatDispositionRecord, String> BUSINESS_PHONE = createField(DSL.name("business_phone"), SQLDataType.VARCHAR(24).nullable(true), this, "");
    public final TableField<ChatDispositionRecord, LocalDateTime> SESSION_STARTED_AT = createField(DSL.name("session_started_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");
    public final TableField<ChatDispositionRecord, LocalDateTime> SESSION_ENDED_AT = createField(DSL.name("session_ended_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");
    public final TableField<ChatDispositionRecord, String> STATUS = createField(DSL.name("status"), SQLDataType.VARCHAR(24).nullable(false), this, "");
    public final TableField<ChatDispositionRecord, Integer> ATTEMPT_COUNT = createField(DSL.name("attempt_count"), SQLDataType.INTEGER.nullable(false), this, "");
    public final TableField<ChatDispositionRecord, String> SUMMARY = createField(DSL.name("summary"), SQLDataType.CLOB.nullable(true), this, "");
    public final TableField<ChatDispositionRecord, String> LEAD_INTENT = createField(DSL.name("lead_intent"), SQLDataType.CLOB.nullable(true), this, "");
    public final TableField<ChatDispositionRecord, String> AGENT_ACTION = createField(DSL.name("agent_action"), SQLDataType.CLOB.nullable(true), this, "");
    public final TableField<ChatDispositionRecord, String> SENTIMENT = createField(DSL.name("sentiment"), SQLDataType.VARCHAR(24).nullable(true), this, "");
    public final TableField<ChatDispositionRecord, String> UNRESOLVED = createField(DSL.name("unresolved"), SQLDataType.CLOB.nullable(true), this, "");
    public final TableField<ChatDispositionRecord, String> KEY_ENTITIES_JSON = createField(DSL.name("key_entities_json"), SQLDataType.CLOB.nullable(true), this, "");
    public final TableField<ChatDispositionRecord, String> MODEL_VERSION = createField(DSL.name("model_version"), SQLDataType.VARCHAR(128).nullable(true), this, "");
    public final TableField<ChatDispositionRecord, String> FAILURE_REASON = createField(DSL.name("failure_reason"), SQLDataType.CLOB.nullable(true), this, "");
    public final TableField<ChatDispositionRecord, LocalDateTime> CRM_DELIVERED_AT = createField(DSL.name("crm_delivered_at"), SQLDataType.LOCALDATETIME(3).nullable(true), this, "");
    public final TableField<ChatDispositionRecord, LocalDateTime> CONVERSE_DELIVERED_AT = createField(DSL.name("converse_delivered_at"), SQLDataType.LOCALDATETIME(3).nullable(true), this, "");
    public final TableField<ChatDispositionRecord, LocalDateTime> CREATED_AT = createField(DSL.name("created_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");
    public final TableField<ChatDispositionRecord, LocalDateTime> UPDATED_AT = createField(DSL.name("updated_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");

    private ChatDisposition() { this(DSL.name("chat_disposition"), null); }
    private ChatDisposition(Name alias, Table<ChatDispositionRecord> aliased) { super(alias, null, aliased, null, DSL.comment(""), TableOptions.table()); }

    @Override public Class<ChatDispositionRecord> getRecordType() { return ChatDispositionRecord.class; }
    @Override public ChatDisposition as(String alias) { return new ChatDisposition(DSL.name(alias), this); }
    @Override public ChatDisposition as(Name alias) { return new ChatDisposition(alias, this); }
    @Override public UniqueKey<ChatDispositionRecord> getPrimaryKey() { return null; }
}
