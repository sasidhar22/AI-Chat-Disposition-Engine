package com.converse.disposition.jooq.generated.tables.records;

import com.converse.disposition.jooq.generated.tables.Lead;
import org.jooq.impl.UpdatableRecordImpl;
import java.time.LocalDateTime;

@SuppressWarnings({"all","unchecked","rawtypes"})
public class LeadRecord extends UpdatableRecordImpl<LeadRecord> {
    private static final long serialVersionUID = 1L;
    public void setLeadId(String v) { set(0, v); }    public String getLeadId()    { return (String) get(0); }
    public void setTenantId(String v) { set(1, v); }  public String getTenantId()  { return (String) get(1); }
    public void setLeadName(String v) { set(2, v); }  public String getLeadName()  { return (String) get(2); }
    public void setLeadPhone(String v) { set(3, v); } public String getLeadPhone() { return (String) get(3); }
    public void setEmail(String v) { set(4, v); }     public String getEmail()     { return (String) get(4); }
    public void setCreatedAt(LocalDateTime v) { set(5, v); } public LocalDateTime getCreatedAt() { return (LocalDateTime) get(5); }
    public void setUpdatedAt(LocalDateTime v) { set(6, v); } public LocalDateTime getUpdatedAt() { return (LocalDateTime) get(6); }
    public LeadRecord() { super(Lead.LEAD); }
}
