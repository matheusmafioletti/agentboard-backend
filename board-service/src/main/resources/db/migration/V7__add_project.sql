CREATE TABLE project (
  id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
  tenant_id            UUID         NOT NULL,
  name                 VARCHAR(255) NOT NULL,
  constitution_content TEXT,
  api_key              VARCHAR(255) NOT NULL,
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_project PRIMARY KEY (id),
  CONSTRAINT uq_project_api_key UNIQUE (api_key),
  CONSTRAINT uq_project_name_per_tenant UNIQUE (tenant_id, name)
);

CREATE INDEX idx_project_tenant ON project (tenant_id);
CREATE UNIQUE INDEX idx_project_api_key ON project (api_key);
