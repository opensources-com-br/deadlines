CREATE FUNCTION grant_custom_permission_to_organization_owner()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT role.id, NEW.id
    FROM roles role
    WHERE role.organization_id = NEW.organization_id
      AND role.key = 'owner'
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END;
$$;

CREATE TRIGGER permissions_grant_to_organization_owner
AFTER INSERT ON permissions
FOR EACH ROW
WHEN (NEW.organization_id IS NOT NULL)
EXECUTE FUNCTION grant_custom_permission_to_organization_owner();
