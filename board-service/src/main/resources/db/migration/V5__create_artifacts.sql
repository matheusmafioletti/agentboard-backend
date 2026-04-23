CREATE TABLE artifact (
  id               UUID         NOT NULL DEFAULT gen_random_uuid(),
  feature_card_id  UUID         NOT NULL REFERENCES feature_card(id) ON DELETE CASCADE,
  tenant_id        UUID         NOT NULL,
  command          VARCHAR(50)  NOT NULL CHECK (command IN ('specify','clarify','plan','tasks','implement')),
  content          TEXT         NOT NULL,
  agent_identifier VARCHAR(255),
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT artifact_pk PRIMARY KEY (id)
);

CREATE INDEX idx_artifact_feature_card_tenant ON artifact(feature_card_id, tenant_id);
