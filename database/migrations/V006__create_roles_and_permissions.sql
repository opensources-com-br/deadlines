CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT permissions_scope_check CHECK (
        (is_system = TRUE AND organization_id IS NULL)
        OR (is_system = FALSE AND organization_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX permissions_system_key_unique
    ON permissions (key)
    WHERE organization_id IS NULL;

CREATE UNIQUE INDEX permissions_organization_key_unique
    ON permissions (organization_id, key)
    WHERE organization_id IS NOT NULL;

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    key VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX roles_organization_key_unique
    ON roles (organization_id, key);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO permissions (id, key, name, description, is_system) VALUES
    ('10000000-0000-0000-0000-000000000001', 'organization.read', 'View organization', 'View organization details.', TRUE),
    ('10000000-0000-0000-0000-000000000002', 'organization.update', 'Update organization', 'Update organization settings.', TRUE),
    ('10000000-0000-0000-0000-000000000003', 'members.read', 'View members', 'View organization members.', TRUE),
    ('10000000-0000-0000-0000-000000000004', 'members.manage', 'Manage members', 'Invite, update, and remove members.', TRUE),
    ('10000000-0000-0000-0000-000000000005', 'roles.read', 'View roles', 'View organization roles.', TRUE),
    ('10000000-0000-0000-0000-000000000006', 'roles.manage', 'Manage roles', 'Create and update organization roles.', TRUE),
    ('10000000-0000-0000-0000-000000000007', 'permissions.read', 'View permissions', 'View available permissions.', TRUE),
    ('10000000-0000-0000-0000-000000000008', 'permissions.manage', 'Manage permissions', 'Create and update custom permissions.', TRUE);

INSERT INTO roles (id, organization_id, key, name, description, is_system)
SELECT
    md5(organization.id::text || ':owner')::uuid,
    organization.id,
    'owner',
    'Owner',
    'Full access to the organization.',
    TRUE
FROM organizations organization;

INSERT INTO roles (id, organization_id, key, name, description, is_system)
SELECT
    md5(organization.id::text || ':member')::uuid,
    organization.id,
    'member',
    'Member',
    'Default access for organization members.',
    TRUE
FROM organizations organization;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.key = 'owner' AND permission.is_system = TRUE;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.key IN ('organization.read', 'members.read')
WHERE role.key = 'member';
