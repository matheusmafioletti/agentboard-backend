CREATE TYPE user_story_stage AS ENUM ('READY', 'IN_PROGRESS', 'DONE');

CREATE TABLE user_story (
  id          UUID              NOT NULL DEFAULT gen_random_uuid(),
  feature_id  UUID              NOT NULL REFERENCES feature(id) ON DELETE CASCADE,
  project_id  UUID              NOT NULL REFERENCES project(id),
  tenant_id   UUID              NOT NULL,
  title       VARCHAR(255)      NOT NULL,
  description TEXT,
  priority    INTEGER           NOT NULL,
  stage       user_story_stage  NOT NULL DEFAULT 'READY',
  "order"     INTEGER           NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ       NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ       NOT NULL DEFAULT now(),
  CONSTRAINT pk_user_story PRIMARY KEY (id)
);

CREATE INDEX idx_us_feature ON user_story (feature_id);
CREATE INDEX idx_us_tenant_stage ON user_story (tenant_id, feature_id, stage);
