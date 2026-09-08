ALTER TABLE roles
    ADD CONSTRAINT roles_organization_id_id_unique UNIQUE (organization_id, id);

ALTER TABLE organization_memberships
    ADD CONSTRAINT organization_memberships_scoped_role_fk
    FOREIGN KEY (organization_id, role_id)
    REFERENCES roles (organization_id, id)
    ON DELETE RESTRICT;

ALTER TABLE organization_invitations
    ADD CONSTRAINT organization_invitations_scoped_role_fk
    FOREIGN KEY (organization_id, role_id)
    REFERENCES roles (organization_id, id)
    ON DELETE RESTRICT;
