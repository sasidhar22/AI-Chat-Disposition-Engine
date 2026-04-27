package com.converse.disposition.jooq.generated.tables.records;

import com.converse.disposition.jooq.generated.tables.Agent;
import org.jooq.impl.UpdatableRecordImpl;
import java.time.LocalDateTime;

@SuppressWarnings({"all","unchecked","rawtypes"})
public class AgentRecord extends UpdatableRecordImpl<AgentRecord> {
    private static final long serialVersionUID = 1L;
    public void setAgentId(String v) { set(0, v); }   public String getAgentId()   { return (String) get(0); }
    public void setTenantId(String v) { set(1, v); }  public String getTenantId()  { return (String) get(1); }
    public void setAgentName(String v) { set(2, v); } public String getAgentName() { return (String) get(2); }
    public void setEmail(String v) { set(3, v); }     public String getEmail()     { return (String) get(3); }
    public void setCreatedAt(LocalDateTime v) { set(4, v); } public LocalDateTime getCreatedAt() { return (LocalDateTime) get(4); }
    public void setUpdatedAt(LocalDateTime v) { set(5, v); } public LocalDateTime getUpdatedAt() { return (LocalDateTime) get(5); }
    public AgentRecord() { super(Agent.AGENT); }
}
