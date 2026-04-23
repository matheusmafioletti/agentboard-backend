CREATE TABLE feature_card (
  id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
  column_id             UUID         NOT NULL,
  tenant_id             UUID         NOT NULL,
  title                 VARCHAR(255) NOT NULL,
  description           TEXT,
  re_execution_pending  BOOLEAN      NOT NULL DEFAULT FALSE,
  display_order         INTEGER      NOT NULL,
  created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_feature_card PRIMARY KEY (id),
  CONSTRAINT fk_feature_card_column FOREIGN KEY (column_id) REFERENCES column_def (id) ON DELETE CASCADE
);

CREATE INDEX idx_feature_card_tenant_column ON feature_card (tenant_id, column_id);
CREATE INDEX idx_feature_card_tenant        ON feature_card (tenant_id);
