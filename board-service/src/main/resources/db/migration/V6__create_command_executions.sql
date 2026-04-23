CREATE TABLE command_execution (
  id               UUID        NOT NULL DEFAULT gen_random_uuid(),
  feature_card_id  UUID        NOT NULL REFERENCES feature_card(id) ON DELETE CASCADE,
  tenant_id        UUID        NOT NULL,
  command          VARCHAR(50) NOT NULL,
  status           VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'ERROR')),
  agent_identifier VARCHAR(255),
  error_message    TEXT,
  started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at      TIMESTAMPTZ,
  duration_ms      BIGINT,
  CONSTRAINT command_execution_pk PRIMARY KEY (id)
);

CREATE INDEX idx_cmd_exec_feature_card_tenant ON command_execution(feature_card_id, tenant_id);
CREATE INDEX idx_cmd_exec_tenant_started      ON command_execution(tenant_id, started_at DESC);
