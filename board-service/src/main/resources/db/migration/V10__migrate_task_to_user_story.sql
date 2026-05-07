-- Drop the old feature_card_id FK before adding user_story_id
ALTER TABLE task DROP CONSTRAINT IF EXISTS task_feature_card_id_fkey;

-- Add user_story_id column
ALTER TABLE task
  ADD COLUMN user_story_id UUID REFERENCES user_story(id) ON DELETE CASCADE;

-- Remove columns no longer needed in the new model
ALTER TABLE task
  DROP COLUMN IF EXISTS feature_card_id,
  DROP COLUMN IF EXISTS tenant_id,
  DROP COLUMN IF EXISTS description,
  DROP COLUMN IF EXISTS priority,
  DROP COLUMN IF EXISTS blocked_reason,
  ADD COLUMN IF NOT EXISTS block_reason TEXT,
  DROP COLUMN IF EXISTS completed_at;

-- Rename blocked_reason to block_reason if it exists (handle both cases)
-- NOTE: block_reason was already added above; blocked_reason was dropped

-- Rename the task index
DROP INDEX IF EXISTS idx_task_feature_card_tenant;
DROP INDEX IF EXISTS idx_task_tenant_completed;

CREATE INDEX idx_task_user_story ON task (user_story_id);
