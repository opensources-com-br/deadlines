-- Identifiers deliberately have no cascading FKs: history survives resource deletion.
CREATE TABLE organization_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    actor_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    action VARCHAR(80) NOT NULL,
    resource VARCHAR(40) NOT NULL,
    resource_id UUID NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT audit_metadata_object CHECK (jsonb_typeof(metadata) = 'object')
);
CREATE INDEX organization_audit_logs_timeline_idx
    ON organization_audit_logs (organization_id, occurred_at DESC, id DESC);
CREATE INDEX organization_audit_logs_action_idx
    ON organization_audit_logs (organization_id, action, occurred_at DESC);

CREATE FUNCTION protect_organization_audit_logs() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Organization audit logs are immutable' USING ERRCODE = '55000';
END;
$$;
CREATE TRIGGER organization_audit_logs_immutable
BEFORE UPDATE OR DELETE OR TRUNCATE ON organization_audit_logs
FOR EACH STATEMENT EXECUTE FUNCTION protect_organization_audit_logs();

-- The API supplies the actor as a transaction-local setting. Direct maintenance
-- writes are still recorded, with a null actor. Never serialize whole rows here.
CREATE FUNCTION record_organization_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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
        IF NEW.status = 'removed' AND OLD.status <> 'removed' THEN event := 'removed';
        ELSIF NEW.role_id IS DISTINCT FROM OLD.role_id THEN event := 'role_updated';
        ELSE RETURN NULL; END IF;
        safe := jsonb_build_object('userId', NEW.user_id, 'previousRoleId', OLD.role_id, 'roleId', NEW.role_id);
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
CREATE TRIGGER audit_organization AFTER UPDATE ON organizations
FOR EACH ROW EXECUTE FUNCTION record_organization_audit();
CREATE TRIGGER audit_member AFTER UPDATE ON organization_memberships
FOR EACH ROW EXECUTE FUNCTION record_organization_audit();
CREATE TRIGGER audit_invitation AFTER INSERT OR UPDATE ON organization_invitations
FOR EACH ROW EXECUTE FUNCTION record_organization_audit();
CREATE TRIGGER audit_role AFTER INSERT OR UPDATE OR DELETE ON roles
FOR EACH ROW EXECUTE FUNCTION record_organization_audit();
CREATE TRIGGER audit_permission AFTER INSERT OR UPDATE OR DELETE ON permissions
FOR EACH ROW EXECUTE FUNCTION record_organization_audit();
-- BEFORE DELETE on roles keeps the organization available for association removal
-- caused by a permission deletion; deleting a role itself has its own event.
CREATE TRIGGER audit_role_permission AFTER INSERT OR DELETE ON role_permissions
FOR EACH ROW EXECUTE FUNCTION record_organization_audit();
