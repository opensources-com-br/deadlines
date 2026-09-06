CREATE FUNCTION provision_default_organization_roles()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    owner_role_id UUID := md5(NEW.id::text || ':owner')::uuid;
    member_role_id UUID := md5(NEW.id::text || ':member')::uuid;
BEGIN
    INSERT INTO roles (id, organization_id, key, name, description, is_system)
    VALUES
        (owner_role_id, NEW.id, 'owner', 'Owner', 'Full access to the organization.', TRUE),
        (member_role_id, NEW.id, 'member', 'Member', 'Default access for organization members.', TRUE);

    INSERT INTO role_permissions (role_id, permission_id)
    SELECT owner_role_id, permission.id
    FROM permissions permission
    WHERE permission.is_system = TRUE;

    INSERT INTO role_permissions (role_id, permission_id)
    SELECT member_role_id, permission.id
    FROM permissions permission
    WHERE permission.key IN ('organization.read', 'members.read')
      AND permission.is_system = TRUE;

    RETURN NEW;
END;
$$;

CREATE TRIGGER organizations_provision_default_roles
AFTER INSERT ON organizations
FOR EACH ROW
EXECUTE FUNCTION provision_default_organization_roles();
