-- Add work_item_id column (nullable initially for back-fill)
ALTER TABLE command_execution
  ADD COLUMN work_item_id UUID;

-- Back-fill: copy feature_id → work_item_id (IDs are identical in work_item after V14)
UPDATE command_execution
SET work_item_id = feature_id
WHERE feature_id IS NOT NULL;

-- Back-fill: copy user_story_id → work_item_id (IDs are identical in work_item after V14)
UPDATE command_execution
SET work_item_id = user_story_id
WHERE user_story_id IS NOT NULL AND work_item_id IS NULL;

-- Drop the XOR constraint that conflicts with the new single-column schema
ALTER TABLE command_execution
  DROP CONSTRAINT IF EXISTS chk_ce_scope;

-- Enforce NOT NULL on the new column now that back-fill is complete
ALTER TABLE command_execution
  ALTER COLUMN work_item_id SET NOT NULL;

-- Add FK to work_item
ALTER TABLE command_execution
  ADD CONSTRAINT fk_command_execution_work_item
    FOREIGN KEY (work_item_id) REFERENCES work_item(id);

-- Drop the old FK constraints and columns
ALTER TABLE command_execution
  DROP CONSTRAINT IF EXISTS fk_ce_feature,
  DROP CONSTRAINT IF EXISTS fk_ce_user_story;

ALTER TABLE command_execution
  DROP COLUMN feature_id,
  DROP COLUMN user_story_id;

DROP INDEX IF EXISTS idx_ce_feature;
DROP INDEX IF EXISTS idx_ce_user_story;
CREATE INDEX idx_ce_work_item ON command_execution(work_item_id);
