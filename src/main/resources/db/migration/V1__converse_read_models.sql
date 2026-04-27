CREATE TABLE IF NOT EXISTS chat_session (
    session_id        BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id         VARCHAR(64)  NOT NULL,
    lead_id           VARCHAR(64)  NOT NULL,
    agent_id          VARCHAR(64)  NOT NULL,
    channel           VARCHAR(24)  NOT NULL COMMENT 'WHATSAPP | SMS | WEB_BOT',
    business_phone    VARCHAR(24)  NULL,
    session_status    VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | CLOSED | EXPIRED',
    close_reason      VARCHAR(32)  NULL COMMENT 'AGENT_CLOSED | AUTO_EXPIRED',
    tenant_industry   VARCHAR(32)  NOT NULL DEFAULT 'OTHER',
    session_started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    session_ended_at  DATETIME(3)  NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (session_id),
    INDEX idx_cs_tenant_lead (tenant_id, lead_id),
    INDEX idx_cs_status (session_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lead (
    lead_id           VARCHAR(64)  NOT NULL,
    tenant_id         VARCHAR(64)  NOT NULL,
    lead_name         VARCHAR(255) NOT NULL,
    lead_phone        VARCHAR(24)  NULL,
    email             VARCHAR(255) NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (lead_id),
    INDEX idx_lead_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent (
    agent_id          VARCHAR(64)  NOT NULL,
    tenant_id         VARCHAR(64)  NOT NULL,
    agent_name        VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (agent_id),
    INDEX idx_agent_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
