ALTER TABLE artifact
  RENAME COLUMN feature_id TO work_item_id;

ALTER TABLE artifact
  DROP CONSTRAINT IF EXISTS fk_artifact_feature;

ALTER TABLE artifact
  ADD CONSTRAINT fk_artifact_work_item
    FOREIGN KEY (work_item_id) REFERENCES work_item(id);

DROP INDEX IF EXISTS idx_artifact_feature;
DROP INDEX IF EXISTS idx_artifact_feature_command;
CREATE INDEX idx_artifact_work_item         ON artifact(work_item_id);
CREATE INDEX idx_artifact_work_item_command ON artifact(work_item_id, command);
