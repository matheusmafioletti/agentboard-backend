ALTER TABLE work_item
  ADD COLUMN display_key VARCHAR(32);

UPDATE work_item
SET display_key = type || '-' ||
    lower(substring(replace(cast(id AS text), '-', '') from 1 for 8));

ALTER TABLE work_item
  ALTER COLUMN display_key SET NOT NULL;

CREATE UNIQUE INDEX uq_work_item_tenant_project_display_key
  ON work_item (tenant_id, project_id, display_key);
