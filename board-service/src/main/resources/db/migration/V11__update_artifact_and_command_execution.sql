-- Update artifact: rename feature_card_id → feature_id
ALTER TABLE artifact RENAME COLUMN feature_card_id TO feature_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS agent_identifier;
ALTER TABLE artifact ALTER COLUMN command TYPE VARCHAR(100);

-- Fix the FK reference (was pointing to renamed table; re-establish to be explicit)
ALTER TABLE artifact DROP CONSTRAINT IF EXISTS artifact_feature_card_id_fkey;
ALTER TABLE artifact ADD CONSTRAINT fk_artifact_feature FOREIGN KEY (feature_id) REFERENCES feature(id) ON DELETE CASCADE;

DROP INDEX IF EXISTS idx_artifact_feature_card_tenant;
CREATE INDEX idx_artifact_feature ON artifact (feature_id);
CREATE INDEX idx_artifact_feature_command ON artifact (feature_id, command);

-- Update command_execution: add project_id and user_story_id; rename feature_card_id → feature_id
ALTER TABLE command_execution
  ADD COLUMN project_id UUID REFERENCES project(id),
  ADD COLUMN user_story_id UUID REFERENCES user_story(id),
  ADD COLUMN agent_id VARCHAR(255);

ALTER TABLE command_execution RENAME COLUMN feature_card_id TO feature_id;

-- Drop old columns
ALTER TABLE command_execution
  DROP COLUMN IF EXISTS tenant_id,
  DROP COLUMN IF EXISTS agent_identifier,
  DROP COLUMN IF EXISTS error_message,
  DROP COLUMN IF EXISTS duration_ms;

-- Fix status values: old schema used 'SUCCESS'/'ERROR'; new schema uses 'COMPLETED'/'FAILED'
UPDATE command_execution SET status = 'COMPLETED' WHERE status = 'SUCCESS';
UPDATE command_execution SET status = 'FAILED' WHERE status = 'ERROR';

ALTER TABLE command_execution DROP CONSTRAINT IF EXISTS command_execution_feature_card_id_fkey;
ALTER TABLE command_execution ADD CONSTRAINT fk_ce_feature FOREIGN KEY (feature_id) REFERENCES feature(id);

-- Add the XOR constraint: exactly one of feature_id or user_story_id must be non-null
ALTER TABLE command_execution ADD CONSTRAINT chk_ce_scope CHECK (
  (feature_id IS NOT NULL AND user_story_id IS NULL)
  OR (feature_id IS NULL AND user_story_id IS NOT NULL)
);

DROP INDEX IF EXISTS idx_cmd_exec_feature_card_tenant;
DROP INDEX IF EXISTS idx_cmd_exec_tenant_started;
CREATE INDEX idx_ce_feature ON command_execution (feature_id);
CREATE INDEX idx_ce_user_story ON command_execution (user_story_id);
CREATE INDEX idx_ce_project_status ON command_execution (project_id, status);
