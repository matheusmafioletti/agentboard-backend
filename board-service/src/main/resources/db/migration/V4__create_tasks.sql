CREATE TABLE task (
  id              UUID         NOT NULL DEFAULT gen_random_uuid(),
  feature_card_id UUID         NOT NULL REFERENCES feature_card(id) ON DELETE CASCADE,
  tenant_id       UUID         NOT NULL,
  title           VARCHAR(500) NOT NULL,
  description     TEXT,
  priority        VARCHAR(5)   NOT NULL CHECK (priority IN ('P1', 'P2', 'P3')),
  completed       BOOLEAN      NOT NULL DEFAULT FALSE,
  blocked         BOOLEAN      NOT NULL DEFAULT FALSE,
  blocked_reason  TEXT,
  completed_at    TIMESTAMPTZ,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT task_pk PRIMARY KEY (id)
);

CREATE INDEX idx_task_feature_card_tenant ON task(feature_card_id, tenant_id);
CREATE INDEX idx_task_tenant_completed   ON task(tenant_id, completed);
