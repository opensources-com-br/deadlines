INSERT INTO permissions (id, key, name, description, is_system) VALUES
    ('10000000-0000-0000-0000-000000000009', 'members.invite', 'Invite members', 'Invite members to the organization.', TRUE),
    ('10000000-0000-0000-0000-000000000010', 'members.update', 'Update members', 'Update organization member access.', TRUE),
    ('10000000-0000-0000-0000-000000000011', 'members.remove', 'Remove members', 'Remove members from the organization.', TRUE),
    ('10000000-0000-0000-0000-000000000012', 'roles.create', 'Create roles', 'Create organization roles.', TRUE),
    ('10000000-0000-0000-0000-000000000013', 'roles.update', 'Update roles', 'Update organization roles.', TRUE),
    ('10000000-0000-0000-0000-000000000014', 'roles.delete', 'Delete roles', 'Delete organization roles.', TRUE),
    ('10000000-0000-0000-0000-000000000015', 'permissions.create', 'Create permissions', 'Create organization permissions.', TRUE),
    ('10000000-0000-0000-0000-000000000016', 'permissions.update', 'Update permissions', 'Update organization permissions.', TRUE),
    ('10000000-0000-0000-0000-000000000017', 'permissions.delete', 'Delete permissions', 'Delete organization permissions.', TRUE),
    ('10000000-0000-0000-0000-000000000018', 'audit.read', 'View audit history', 'View organization audit history.', TRUE),
    ('10000000-0000-0000-0000-000000000019', 'billing.read', 'View billing', 'View organization billing and plan details.', TRUE),
    ('10000000-0000-0000-0000-000000000020', 'billing.manage', 'Manage billing', 'Manage organization billing and subscriptions.', TRUE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.key = 'owner'
  AND permission.key IN (
      'members.invite', 'members.update', 'members.remove',
      'roles.create', 'roles.update', 'roles.delete',
      'permissions.create', 'permissions.update', 'permissions.delete',
      'audit.read', 'billing.read', 'billing.manage'
  )
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT existing.role_id, granular.id
FROM role_permissions existing
JOIN permissions legacy ON legacy.id = existing.permission_id
JOIN permissions granular ON granular.key IN (
    CASE legacy.key
        WHEN 'members.manage' THEN 'members.invite'
        WHEN 'roles.manage' THEN 'roles.create'
        WHEN 'permissions.manage' THEN 'permissions.create'
    END,
    CASE legacy.key
        WHEN 'members.manage' THEN 'members.update'
        WHEN 'roles.manage' THEN 'roles.update'
        WHEN 'permissions.manage' THEN 'permissions.update'
    END,
    CASE legacy.key
        WHEN 'members.manage' THEN 'members.remove'
        WHEN 'roles.manage' THEN 'roles.delete'
        WHEN 'permissions.manage' THEN 'permissions.delete'
    END
)
WHERE legacy.key IN ('members.manage', 'roles.manage', 'permissions.manage')
ON CONFLICT DO NOTHING;

DELETE FROM permissions
WHERE is_system = TRUE
  AND key IN ('members.manage', 'roles.manage', 'permissions.manage');
