ALTER TABLE project
    ADD COLUMN data_source VARCHAR(20) NOT NULL DEFAULT 'manual';

ALTER TABLE work_item
    ADD COLUMN data_source VARCHAR(20) NOT NULL DEFAULT 'manual';

UPDATE project SET data_source = 'manual' WHERE data_source IS NULL;
UPDATE work_item SET data_source = 'manual' WHERE data_source IS NULL;

ALTER TABLE project
    ADD CONSTRAINT chk_project_data_source
    CHECK (data_source IN ('manual', 'automation', 'seed'));

ALTER TABLE work_item
    ADD CONSTRAINT chk_work_item_data_source
    CHECK (data_source IN ('manual', 'automation', 'seed'));
