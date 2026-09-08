CREATE TABLE email_change_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    new_email VARCHAR(320) NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX email_change_requests_active_user_unique
    ON email_change_requests (user_id)
    WHERE confirmed_at IS NULL;

CREATE UNIQUE INDEX email_change_requests_active_email_unique
    ON email_change_requests (LOWER(new_email))
    WHERE confirmed_at IS NULL;
