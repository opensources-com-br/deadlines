CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX organizations_slug_normalized_unique
    ON organizations (LOWER(slug));

CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMPTZ,
    CONSTRAINT organization_memberships_role_check CHECK (role IN ('owner', 'member')),
    CONSTRAINT organization_memberships_status_check CHECK (status IN ('active', 'removed')),
    CONSTRAINT organization_memberships_removed_at_check CHECK (
        (status = 'active' AND removed_at IS NULL)
        OR (status = 'removed' AND removed_at IS NOT NULL)
    )
);

CREATE INDEX organization_memberships_organization_idx
    ON organization_memberships (organization_id);

CREATE UNIQUE INDEX organization_memberships_one_active_per_user
    ON organization_memberships (user_id)
    WHERE status = 'active';

CREATE UNIQUE INDEX organization_memberships_one_active_owner
    ON organization_memberships (organization_id)
    WHERE status = 'active' AND role = 'owner';
