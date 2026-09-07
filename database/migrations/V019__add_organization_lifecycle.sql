ALTER TABLE organizations
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'active',
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD CONSTRAINT organizations_status_check CHECK (status IN ('active', 'suspended', 'deleted')),
    ADD CONSTRAINT organizations_deleted_at_check CHECK (
        (status IN ('active', 'suspended') AND deleted_at IS NULL)
        OR (status = 'deleted' AND deleted_at IS NOT NULL)
    );

DROP INDEX organizations_slug_normalized_unique;

CREATE UNIQUE INDEX organizations_retained_slug_normalized_unique
    ON organizations (LOWER(slug))
    WHERE status <> 'deleted';

CREATE OR REPLACE FUNCTION ensure_organization_has_one_active_owner() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    affected_organization_id UUID := COALESCE(NEW.organization_id, OLD.organization_id);
    owner_count INTEGER;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM organizations
        WHERE id = affected_organization_id AND status <> 'deleted'
    ) THEN
        RETURN NULL;
    END IF;

    SELECT COUNT(*) INTO owner_count
    FROM organization_memberships
    WHERE organization_id = affected_organization_id
      AND status = 'active'
      AND role = 'owner';

    IF owner_count <> 1 THEN
        RAISE EXCEPTION 'Organization must have exactly one active owner'
            USING ERRCODE = '23514',
                  CONSTRAINT = 'organization_memberships_exactly_one_active_owner';
    END IF;
    RETURN NULL;
END;
$$;

CREATE FUNCTION record_organization_lifecycle_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    event TEXT;
BEGIN
    IF NEW.status IS NOT DISTINCT FROM OLD.status THEN RETURN NULL; END IF;

    event := CASE NEW.status
        WHEN 'suspended' THEN 'suspended'
        WHEN 'active' THEN 'reactivated'
        WHEN 'deleted' THEN 'deleted'
    END;

    INSERT INTO organization_audit_logs (organization_id, actor_id, action, resource, resource_id, metadata)
    VALUES (
        NEW.id,
        nullif(current_setting('deadlines.audit_actor', true), '')::uuid,
        'organization.' || event,
        'organization',
        NEW.id,
        jsonb_build_object('previousStatus', OLD.status, 'status', NEW.status)
    );
    RETURN NULL;
END;
$$;

CREATE TRIGGER audit_organization_lifecycle
AFTER UPDATE OF status ON organizations
FOR EACH ROW EXECUTE FUNCTION record_organization_lifecycle_audit();
