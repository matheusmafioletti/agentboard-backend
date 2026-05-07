-- Drop trigger and function that depend on column_def before we alter feature_card
DROP TRIGGER IF EXISTS trg_seed_board_columns ON board;

-- Create the feature_stage enum
CREATE TYPE feature_stage AS ENUM (
  'BACKLOG', 'SPECIFY', 'CLARIFY', 'PLAN', 'TASKS',
  'READY', 'IN_DEVELOPMENT', 'PR_REVIEW', 'DONE'
);

-- Add project_id and stage to feature_card before renaming
ALTER TABLE feature_card
  ADD COLUMN project_id UUID REFERENCES project(id),
  ADD COLUMN stage feature_stage NOT NULL DEFAULT 'BACKLOG';

-- Rename the table
ALTER TABLE feature_card RENAME TO feature;

-- Remove the old column_id FK dependency (column_id becomes optional legacy reference)
ALTER TABLE feature DROP CONSTRAINT IF EXISTS fk_feature_card_column;
ALTER TABLE feature DROP COLUMN IF EXISTS column_id;
ALTER TABLE feature DROP COLUMN IF EXISTS re_execution_pending;
ALTER TABLE feature DROP COLUMN IF EXISTS display_order;

-- Rename index
DROP INDEX IF EXISTS idx_feature_card_tenant_column;
DROP INDEX IF EXISTS idx_feature_card_tenant;

CREATE INDEX idx_feature_project ON feature (project_id);
CREATE INDEX idx_feature_tenant_stage ON feature (tenant_id, stage);
