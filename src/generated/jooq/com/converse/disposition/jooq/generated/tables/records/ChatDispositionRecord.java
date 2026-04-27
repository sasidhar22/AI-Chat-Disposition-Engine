package com.converse.disposition.jooq.generated.tables.records;

import com.converse.disposition.jooq.generated.tables.ChatDisposition;
import org.jooq.impl.UpdatableRecordImpl;
import java.time.LocalDateTime;

@SuppressWarnings({"all","unchecked","rawtypes"})
public class ChatDispositionRecord extends UpdatableRecordImpl<ChatDispositionRecord> {
    private static final long serialVersionUID = 1L;
    public void setId(Long v) { set(0, v); }                        public Long getId()                        { return (Long) get(0); }
    public void setSessionId(Long v) { set(1, v); }                 public Long getSessionId()                 { return (Long) get(1); }
    public void setTenantId(String v) { set(2, v); }                public String getTenantId()                { return (String) get(2); }
    public void setLeadId(String v) { set(3, v); }                  public String getLeadId()                  { return (String) get(3); }
    public void setAgentId(String v) { set(4, v); }                 public String getAgentId()                 { return (String) get(4); }
    public void setChannel(String v) { set(5, v); }                 public String getChannel()                 { return (String) get(5); }
    public void setBusinessPhone(String v) { set(6, v); }           public String getBusinessPhone()           { return (String) get(6); }
    public void setSessionStartedAt(LocalDateTime v) { set(7, v); } public LocalDateTime getSessionStartedAt() { return (LocalDateTime) get(7); }
    public void setSessionEndedAt(LocalDateTime v) { set(8, v); }   public LocalDateTime getSessionEndedAt()   { return (LocalDateTime) get(8); }
    public void setStatus(String v) { set(9, v); }                  public String getStatus()                  { return (String) get(9); }
    public void setAttemptCount(Integer v) { set(10, v); }          public Integer getAttemptCount()           { return (Integer) get(10); }
    public void setSummary(String v) { set(11, v); }                public String getSummary()                 { return (String) get(11); }
    public void setLeadIntent(String v) { set(12, v); }             public String getLeadIntent()              { return (String) get(12); }
    public void setAgentAction(String v) { set(13, v); }            public String getAgentAction()             { return (String) get(13); }
    public void setSentiment(String v) { set(14, v); }              public String getSentiment()               { return (String) get(14); }
    public void setUnresolved(String v) { set(15, v); }             public String getUnresolved()              { return (String) get(15); }
    public void setKeyEntitiesJson(String v) { set(16, v); }        public String getKeyEntitiesJson()         { return (String) get(16); }
    public void setModelVersion(String v) { set(17, v); }           public String getModelVersion()            { return (String) get(17); }
    public void setFailureReason(String v) { set(18, v); }          public String getFailureReason()           { return (String) get(18); }
    public void setCrmDeliveredAt(LocalDateTime v) { set(19, v); }  public LocalDateTime getCrmDeliveredAt()   { return (LocalDateTime) get(19); }
    public void setConverseDeliveredAt(LocalDateTime v) { set(20, v); } public LocalDateTime getConverseDeliveredAt() { return (LocalDateTime) get(20); }
    public void setCreatedAt(LocalDateTime v) { set(21, v); }       public LocalDateTime getCreatedAt()        { return (LocalDateTime) get(21); }
    public void setUpdatedAt(LocalDateTime v) { set(22, v); }       public LocalDateTime getUpdatedAt()        { return (LocalDateTime) get(22); }
    public ChatDispositionRecord() { super(ChatDisposition.CHAT_DISPOSITION); }
}
