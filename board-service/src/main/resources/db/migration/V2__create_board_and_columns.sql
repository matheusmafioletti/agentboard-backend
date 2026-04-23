CREATE TABLE board (
  id          UUID         NOT NULL DEFAULT gen_random_uuid(),
  tenant_id   UUID         NOT NULL,
  name        VARCHAR(100) NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_board PRIMARY KEY (id)
);

CREATE INDEX idx_board_tenant ON board (tenant_id);

CREATE TABLE column_def (
  id            UUID        NOT NULL DEFAULT gen_random_uuid(),
  board_id      UUID        NOT NULL,
  tenant_id     UUID        NOT NULL,
  name          VARCHAR(50) NOT NULL,
  stage         VARCHAR(20) NOT NULL,
  display_order SMALLINT    NOT NULL,
  CONSTRAINT pk_column_def PRIMARY KEY (id),
  CONSTRAINT fk_column_def_board FOREIGN KEY (board_id) REFERENCES board (id) ON DELETE CASCADE,
  CONSTRAINT chk_column_def_stage CHECK (
    stage IN ('BACKLOG', 'SPECIFY', 'PLAN', 'IN_PROGRESS', 'REVIEW', 'DONE')
  )
);

CREATE INDEX idx_column_def_board   ON column_def (board_id);
CREATE INDEX idx_column_def_tenant  ON column_def (tenant_id);

CREATE OR REPLACE FUNCTION seed_board_columns()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO column_def (board_id, tenant_id, name, stage, display_order) VALUES
    (NEW.id, NEW.tenant_id, 'Backlog',     'BACKLOG',     0),
    (NEW.id, NEW.tenant_id, 'Specify',     'SPECIFY',     1),
    (NEW.id, NEW.tenant_id, 'Plan',        'PLAN',        2),
    (NEW.id, NEW.tenant_id, 'In Progress', 'IN_PROGRESS', 3),
    (NEW.id, NEW.tenant_id, 'Review',      'REVIEW',      4),
    (NEW.id, NEW.tenant_id, 'Done',        'DONE',        5);
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_seed_board_columns
AFTER INSERT ON board
FOR EACH ROW EXECUTE FUNCTION seed_board_columns();
