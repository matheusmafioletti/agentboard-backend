ALTER TABLE tenant
    ADD COLUMN test_tenant BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE user_account
    ADD COLUMN data_source VARCHAR(20) NOT NULL DEFAULT 'manual';

ALTER TABLE tenant_membership
    ADD COLUMN data_source VARCHAR(20) NOT NULL DEFAULT 'manual';

ALTER TABLE tenant_invite
    ADD COLUMN data_source VARCHAR(20) NOT NULL DEFAULT 'manual';

ALTER TABLE tenant_api_key
    ADD COLUMN data_source VARCHAR(20) NOT NULL DEFAULT 'manual';

UPDATE user_account SET data_source = 'manual' WHERE data_source IS NULL;
UPDATE tenant_membership SET data_source = 'manual' WHERE data_source IS NULL;
UPDATE tenant_invite SET data_source = 'manual' WHERE data_source IS NULL;
UPDATE tenant_api_key SET data_source = 'manual' WHERE data_source IS NULL;

ALTER TABLE user_account
    ADD CONSTRAINT chk_user_account_data_source
    CHECK (data_source IN ('manual', 'automation', 'seed'));

ALTER TABLE tenant_membership
    ADD CONSTRAINT chk_tenant_membership_data_source
    CHECK (data_source IN ('manual', 'automation', 'seed'));

ALTER TABLE tenant_invite
    ADD CONSTRAINT chk_tenant_invite_data_source
    CHECK (data_source IN ('manual', 'automation', 'seed'));

ALTER TABLE tenant_api_key
    ADD CONSTRAINT chk_tenant_api_key_data_source
    CHECK (data_source IN ('manual', 'automation', 'seed'));
