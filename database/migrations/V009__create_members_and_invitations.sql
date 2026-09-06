ALTER TABLE organization_memberships
    ADD COLUMN role_id UUID REFERENCES roles(id) ON DELETE RESTRICT;

UPDATE organization_memberships membership
SET role_id = role.id
FROM roles role
WHERE role.organization_id = membership.organization_id
  AND role.key = membership.role;

CREATE FUNCTION resolve_membership_role_id()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.role_id IS NULL THEN
        SELECT role.id
        INTO NEW.role_id
        FROM roles role
        WHERE role.organization_id = NEW.organization_id
          AND role.key = NEW.role;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER organization_memberships_resolve_role_id
BEFORE INSERT OR UPDATE OF organization_id, role, role_id ON organization_memberships
FOR EACH ROW
EXECUTE FUNCTION resolve_membership_role_id();

ALTER TABLE organization_memberships
    ALTER COLUMN role_id SET NOT NULL;

CREATE INDEX organization_memberships_role_idx
    ON organization_memberships (role_id);

CREATE TABLE organization_invitations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(320) NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    invited_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    token_hash CHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT organization_invitations_status_check CHECK (status IN ('pending', 'accepted', 'revoked')),
    CONSTRAINT organization_invitations_state_check CHECK (
        (status = 'pending' AND accepted_by IS NULL AND accepted_at IS NULL AND revoked_at IS NULL)
        OR (status = 'accepted' AND accepted_by IS NOT NULL AND accepted_at IS NOT NULL AND revoked_at IS NULL)
        OR (status = 'revoked' AND accepted_by IS NULL AND accepted_at IS NULL AND revoked_at IS NOT NULL)
    )
);

CREATE INDEX organization_invitations_organization_idx
    ON organization_invitations (organization_id, created_at DESC);

CREATE INDEX organization_invitations_email_idx
    ON organization_invitations (LOWER(email));

CREATE UNIQUE INDEX organization_invitations_one_pending_per_email
    ON organization_invitations (organization_id, LOWER(email))
    WHERE status = 'pending';
