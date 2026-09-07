ALTER TABLE users
    ADD COLUMN disabled_at TIMESTAMPTZ,
    ADD COLUMN deleted_at TIMESTAMPTZ;

UPDATE users
SET disabled_at = updated_at
WHERE status = 'disabled';

ALTER TABLE users DROP CONSTRAINT users_status_check;

ALTER TABLE users
    ADD CONSTRAINT users_status_check
        CHECK (status IN ('pending', 'active', 'disabled', 'deleted')),
    ADD CONSTRAINT users_account_lifecycle_check
        CHECK (
            (status IN ('pending', 'active') AND disabled_at IS NULL AND deleted_at IS NULL)
            OR (status = 'disabled' AND disabled_at IS NOT NULL AND deleted_at IS NULL)
            OR (status = 'deleted' AND disabled_at IS NOT NULL AND deleted_at IS NOT NULL)
        );
