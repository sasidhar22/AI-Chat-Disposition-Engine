package com.converse.disposition.jooq.generated.tables.records;

import com.converse.disposition.jooq.generated.tables.ChatSession;
import org.jooq.impl.UpdatableRecordImpl;
import java.time.LocalDateTime;

@SuppressWarnings({"all","unchecked","rawtypes"})
public class ChatSessionRecord extends UpdatableRecordImpl<ChatSessionRecord> {
    private static final long serialVersionUID = 1L;
    public void setSessionId(Long v) { set(0, v); }         public Long getSessionId()         { return (Long) get(0); }
    public void setTenantId(String v) { set(1, v); }        public String getTenantId()        { return (String) get(1); }
    public void setLeadId(String v) { set(2, v); }          public String getLeadId()          { return (String) get(2); }
    public void setAgentId(String v) { set(3, v); }         public String getAgentId()         { return (String) get(3); }
    public void setChannel(String v) { set(4, v); }         public String getChannel()         { return (String) get(4); }
    public void setBusinessPhone(String v) { set(5, v); }   public String getBusinessPhone()   { return (String) get(5); }
    public void setSessionStatus(String v) { set(6, v); }   public String getSessionStatus()   { return (String) get(6); }
    public void setCloseReason(String v) { set(7, v); }     public String getCloseReason()     { return (String) get(7); }
    public void setTenantIndustry(String v) { set(8, v); }  public String getTenantIndustry()  { return (String) get(8); }
    public void setSessionStartedAt(LocalDateTime v) { set(9, v); }  public LocalDateTime getSessionStartedAt()  { return (LocalDateTime) get(9); }
    public void setSessionEndedAt(LocalDateTime v) { set(10, v); }   public LocalDateTime getSessionEndedAt()    { return (LocalDateTime) get(10); }
    public void setCreatedAt(LocalDateTime v) { set(11, v); }        public LocalDateTime getCreatedAt()         { return (LocalDateTime) get(11); }
    public void setUpdatedAt(LocalDateTime v) { set(12, v); }        public LocalDateTime getUpdatedAt()         { return (LocalDateTime) get(12); }
    public ChatSessionRecord() { super(ChatSession.CHAT_SESSION); }
}
