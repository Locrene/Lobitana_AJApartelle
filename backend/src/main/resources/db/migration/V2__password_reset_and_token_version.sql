-- AJ Apartelle - Increment 2: password management (FR-045, FR-073, FR-074, FR-075)

-- Copied into the JWT "tv" claim; increasing it signs out every existing session of the user
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;

-- Only the SHA-256 hash of a reset token is stored, never the token itself
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,          -- set when used or invalidated
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_password_reset_tokens_user_created ON password_reset_tokens (user_id, created_at);
CREATE INDEX ix_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);
