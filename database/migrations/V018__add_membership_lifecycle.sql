ALTER TABLE organization_memberships
    DROP CONSTRAINT organization_memberships_status_check,
    DROP CONSTRAINT organization_memberships_removed_at_check;

ALTER TABLE organization_memberships
    ADD CONSTRAINT organization_memberships_status_check
        CHECK (status IN ('active', 'suspended', 'removed')),
    ADD CONSTRAINT organization_memberships_removed_at_check CHECK (
        (status IN ('active', 'suspended') AND removed_at IS NULL)
        OR (status = 'removed' AND removed_at IS NOT NULL)
    );

DROP INDEX organization_memberships_one_active_per_user;

CREATE UNIQUE INDEX organization_memberships_one_retained_per_user
    ON organization_memberships (user_id)
    WHERE status IN ('active', 'suspended');

CREATE FUNCTION ensure_organization_has_one_active_owner() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    affected_organization_id UUID := COALESCE(NEW.organization_id, OLD.organization_id);
    owner_count INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM organizations WHERE id = affected_organization_id) THEN
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

CREATE CONSTRAINT TRIGGER organization_memberships_exactly_one_active_owner
AFTER INSERT OR UPDATE OR DELETE ON organization_memberships
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION ensure_organization_has_one_active_owner();

CREATE OR REPLACE FUNCTION record_organization_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    org UUID;
    target UUID;
    event TEXT;
    kind TEXT;
    safe JSONB := '{}'::jsonb;
    row_data JSONB;
BEGIN
    row_data := CASE WHEN TG_OP = 'DELETE' THEN to_jsonb(OLD) ELSE to_jsonb(NEW) END;
    org := (row_data->>'organization_id')::uuid;
    target := (row_data->>'id')::uuid;
    IF TG_TABLE_NAME = 'organizations' THEN
        IF NEW.name IS NOT DISTINCT FROM OLD.name AND NEW.slug IS NOT DISTINCT FROM OLD.slug THEN RETURN NULL; END IF;
        org := NEW.id;
        kind := 'organization';
        event := 'updated';
        safe := jsonb_build_object('nameChanged', NEW.name IS DISTINCT FROM OLD.name, 'slugChanged', NEW.slug IS DISTINCT FROM OLD.slug);
    ELSIF TG_TABLE_NAME = 'organization_memberships' THEN
        kind := 'member';
        IF NEW.status = 'suspended' AND OLD.status = 'active' THEN event := 'suspended';
        ELSIF NEW.status = 'active' AND OLD.status = 'suspended' THEN event := 'reactivated';
        ELSIF NEW.status = 'removed' AND OLD.status <> 'removed' THEN event := 'removed';
        ELSIF NEW.role_id IS DISTINCT FROM OLD.role_id THEN event := 'role_updated';
        ELSE RETURN NULL; END IF;
        safe := jsonb_build_object('userId', NEW.user_id, 'previousRoleId', OLD.role_id, 'roleId', NEW.role_id,
            'previousStatus', OLD.status, 'status', NEW.status);
    ELSIF TG_TABLE_NAME = 'organization_invitations' THEN
        kind := 'invitation';
        IF TG_OP = 'INSERT' THEN event := 'created';
        ELSIF NEW.status = 'accepted' AND OLD.status <> 'accepted' THEN event := 'accepted';
        ELSIF NEW.status = 'revoked' AND OLD.status <> 'revoked' THEN event := 'revoked';
        ELSIF NEW.token_hash IS DISTINCT FROM OLD.token_hash THEN event := 'resent';
        ELSE RETURN NULL; END IF;
        safe := jsonb_strip_nulls(jsonb_build_object('roleId', NEW.role_id, 'acceptedBy', NEW.accepted_by));
    ELSIF TG_TABLE_NAME = 'role_permissions' THEN
        kind := 'role';
        target := (row_data->>'role_id')::uuid;
        SELECT organization_id INTO org FROM roles WHERE id = target;
        event := CASE WHEN TG_OP = 'INSERT' THEN 'permission_added' ELSE 'permission_removed' END;
        safe := jsonb_build_object('permissionId', row_data->>'permission_id');
    ELSE
        kind := CASE WHEN TG_TABLE_NAME = 'roles' THEN 'role' ELSE 'permission' END;
        event := CASE TG_OP WHEN 'INSERT' THEN 'created' WHEN 'UPDATE' THEN 'updated' ELSE 'deleted' END;
        IF TG_OP = 'UPDATE' THEN
            IF NEW.key IS NOT DISTINCT FROM OLD.key AND NEW.name IS NOT DISTINCT FROM OLD.name
               AND NEW.description IS NOT DISTINCT FROM OLD.description THEN RETURN NULL; END IF;
            safe := jsonb_build_object('keyChanged', NEW.key IS DISTINCT FROM OLD.key,
                'nameChanged', NEW.name IS DISTINCT FROM OLD.name,
                'descriptionChanged', NEW.description IS DISTINCT FROM OLD.description);
        END IF;
    END IF;
    IF org IS NOT NULL THEN
        INSERT INTO organization_audit_logs (organization_id, actor_id, action, resource, resource_id, metadata)
        VALUES (org, nullif(current_setting('deadlines.audit_actor', true), '')::uuid, kind || '.' || event, kind, target, safe);
    END IF;
    RETURN NULL;
END;
$$;
