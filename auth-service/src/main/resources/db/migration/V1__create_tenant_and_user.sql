CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE tenant (
  id          UUID         NOT NULL DEFAULT gen_random_uuid(),
  name        VARCHAR(100) NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_tenant PRIMARY KEY (id),
  CONSTRAINT uq_tenant_name UNIQUE (name)
);

CREATE TABLE user_account (
  id            UUID          NOT NULL DEFAULT gen_random_uuid(),
  tenant_id     UUID          NOT NULL,
  email         VARCHAR(255)  NOT NULL,
  password_hash VARCHAR(255)  NOT NULL,
  roles         VARCHAR(50)[] NOT NULL DEFAULT '{USER}',
  created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CONSTRAINT pk_user_account PRIMARY KEY (id),
  CONSTRAINT uq_user_account_email UNIQUE (email),
  CONSTRAINT fk_user_account_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX idx_user_account_email    ON user_account (email);
CREATE INDEX idx_user_account_tenant   ON user_account (tenant_id);

CREATE TABLE tenant_api_key (
  id          UUID         NOT NULL DEFAULT gen_random_uuid(),
  tenant_id   UUID         NOT NULL,
  key_hash    VARCHAR(255) NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  revoked_at  TIMESTAMPTZ,
  CONSTRAINT pk_tenant_api_key PRIMARY KEY (id),
  CONSTRAINT uq_tenant_api_key_hash UNIQUE (key_hash),
  CONSTRAINT fk_tenant_api_key_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX idx_tenant_api_key_tenant ON tenant_api_key (tenant_id);
