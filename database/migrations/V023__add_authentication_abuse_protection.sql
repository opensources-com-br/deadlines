CREATE TABLE authentication_rate_limits (
    key_hash CHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL CHECK (request_count >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE authentication_login_attempts (
    id UUID PRIMARY KEY,
    email_hash CHAR(64) NOT NULL,
    ip_hash CHAR(64) NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL,
    successful BOOLEAN NOT NULL
);

CREATE INDEX authentication_login_attempts_lookup
    ON authentication_login_attempts (email_hash, ip_hash, attempted_at DESC);
