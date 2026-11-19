-- AJ Apartelle - Increment 1: user accounts and authentication
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    mobile_number  VARCHAR(11),
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(10)  NOT NULL CHECK (role IN ('GUEST', 'STAFF', 'OWNER')),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_mobile CHECK (mobile_number IS NULL OR mobile_number ~ '^09[0-9]{9}$')
);

-- Case-insensitive uniqueness of e-mail, enforced by the database
CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));

-- Server-side logout: a token whose id (jti) is listed here is rejected until it expires
CREATE TABLE revoked_tokens (
    jti         VARCHAR(64) PRIMARY KEY,
    expires_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_revoked_tokens_expires_at ON revoked_tokens (expires_at);
