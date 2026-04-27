package com.converse.disposition.jooq.generated.tables;

import com.converse.disposition.jooq.generated.tables.records.ChatSessionRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import java.time.LocalDateTime;

@SuppressWarnings({"all","unchecked","rawtypes"})
public class ChatSession extends TableImpl<ChatSessionRecord> {
    private static final long serialVersionUID = 1L;
    public static final ChatSession CHAT_SESSION = new ChatSession();

    public final TableField<ChatSessionRecord, Long> SESSION_ID = createField(DSL.name("session_id"), SQLDataType.BIGINT.nullable(false), this, "");
    public final TableField<ChatSessionRecord, String> TENANT_ID = createField(DSL.name("tenant_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<ChatSessionRecord, String> LEAD_ID = createField(DSL.name("lead_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<ChatSessionRecord, String> AGENT_ID = createField(DSL.name("agent_id"), SQLDataType.VARCHAR(64).nullable(false), this, "");
    public final TableField<ChatSessionRecord, String> CHANNEL = createField(DSL.name("channel"), SQLDataType.VARCHAR(24).nullable(false), this, "");
    public final TableField<ChatSessionRecord, String> BUSINESS_PHONE = createField(DSL.name("business_phone"), SQLDataType.VARCHAR(24).nullable(true), this, "");
    public final TableField<ChatSessionRecord, String> SESSION_STATUS = createField(DSL.name("session_status"), SQLDataType.VARCHAR(24).nullable(false), this, "");
    public final TableField<ChatSessionRecord, String> CLOSE_REASON = createField(DSL.name("close_reason"), SQLDataType.VARCHAR(32).nullable(true), this, "");
    public final TableField<ChatSessionRecord, String> TENANT_INDUSTRY = createField(DSL.name("tenant_industry"), SQLDataType.VARCHAR(32).nullable(false), this, "");
    public final TableField<ChatSessionRecord, LocalDateTime> SESSION_STARTED_AT = createField(DSL.name("session_started_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");
    public final TableField<ChatSessionRecord, LocalDateTime> SESSION_ENDED_AT = createField(DSL.name("session_ended_at"), SQLDataType.LOCALDATETIME(3).nullable(true), this, "");
    public final TableField<ChatSessionRecord, LocalDateTime> CREATED_AT = createField(DSL.name("created_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");
    public final TableField<ChatSessionRecord, LocalDateTime> UPDATED_AT = createField(DSL.name("updated_at"), SQLDataType.LOCALDATETIME(3).nullable(false), this, "");

    private ChatSession() { this(DSL.name("chat_session"), null); }
    private ChatSession(Name alias, Table<ChatSessionRecord> aliased) { super(alias, null, aliased, null, DSL.comment(""), TableOptions.table()); }

    @Override public Class<ChatSessionRecord> getRecordType() { return ChatSessionRecord.class; }
    @Override public ChatSession as(String alias) { return new ChatSession(DSL.name(alias), this); }
    @Override public ChatSession as(Name alias) { return new ChatSession(alias, this); }
    @Override public UniqueKey<ChatSessionRecord> getPrimaryKey() { return null; }
}
