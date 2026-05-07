CREATE TABLE work_item (
  id             UUID         NOT NULL DEFAULT gen_random_uuid(),
  tenant_id      UUID         NOT NULL,
  project_id     UUID         NOT NULL,
  type           VARCHAR(20)  NOT NULL,
  title          VARCHAR(255) NOT NULL,
  description    TEXT,
  status         VARCHAR(50)  NOT NULL,
  parent_id      UUID,
  priority       INT          NOT NULL DEFAULT 5,
  display_order  INT          NOT NULL DEFAULT 0,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_work_item         PRIMARY KEY (id),
  CONSTRAINT fk_work_item_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_work_item_parent  FOREIGN KEY (parent_id)  REFERENCES work_item(id),
  CONSTRAINT chk_work_item_type   CHECK (type IN ('FEATURE', 'USER_STORY', 'TASK'))
);

CREATE INDEX idx_work_item_project      ON work_item(project_id);
CREATE INDEX idx_work_item_tenant       ON work_item(tenant_id);
CREATE INDEX idx_work_item_parent       ON work_item(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_work_item_type_project ON work_item(project_id, type);
