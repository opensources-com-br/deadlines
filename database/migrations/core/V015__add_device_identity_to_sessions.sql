DELETE FROM sessions
WHERE revoked_at IS NOT NULL
   OR expires_at <= CURRENT_TIMESTAMP;

ALTER TABLE sessions
    ADD COLUMN device_id UUID,
    ADD COLUMN last_seen_at TIMESTAMPTZ;

UPDATE sessions
SET device_id = id,
    last_seen_at = created_at;

ALTER TABLE sessions
    ALTER COLUMN device_id SET NOT NULL,
    ALTER COLUMN last_seen_at SET NOT NULL;

CREATE UNIQUE INDEX sessions_user_device_unique_idx
    ON sessions (user_id, device_id);

CREATE INDEX sessions_last_seen_idx
    ON sessions (user_id, last_seen_at DESC);
