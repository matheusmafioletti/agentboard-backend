ALTER TABLE user_account ADD COLUMN IF NOT EXISTS name VARCHAR(255);

UPDATE user_account
SET name = split_part(email, '@', 1)
WHERE name IS NULL;

ALTER TABLE user_account ALTER COLUMN name SET NOT NULL;

CREATE TABLE tenant_membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_membership_user_tenant UNIQUE (user_id, tenant_id),
    CONSTRAINT chk_tenant_membership_role CHECK (role IN ('ADMIN', 'USER'))
);

CREATE INDEX idx_tenant_membership_user ON tenant_membership (user_id);
CREATE INDEX idx_tenant_membership_tenant ON tenant_membership (tenant_id);

INSERT INTO tenant_membership (id, user_id, tenant_id, role, joined_at)
SELECT gen_random_uuid(), id, tenant_id, 'ADMIN', created_at
FROM user_account;

CREATE TABLE tenant_invite (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    invited_by UUID NOT NULL REFERENCES user_account(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at TIMESTAMPTZ,
    CONSTRAINT chk_tenant_invite_status CHECK (status IN ('PENDING', 'ACCEPTED', 'CANCELLED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uq_tenant_invite_pending_email
    ON tenant_invite (tenant_id, email)
    WHERE status = 'PENDING';

CREATE INDEX idx_tenant_invite_tenant ON tenant_invite (tenant_id);
CREATE INDEX idx_tenant_invite_email ON tenant_invite (email);

ALTER TABLE user_account DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE user_account DROP COLUMN IF EXISTS roles;

DROP INDEX IF EXISTS idx_user_account_tenant;
