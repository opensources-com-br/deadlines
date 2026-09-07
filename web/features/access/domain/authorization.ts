export const platformPermission = {
  organizationRead: "organization.read",
  organizationUpdate: "organization.update",
  membersRead: "members.read",
  membersInvite: "members.invite",
  membersUpdate: "members.update",
  membersRemove: "members.remove",
  rolesRead: "roles.read",
  rolesCreate: "roles.create",
  rolesUpdate: "roles.update",
  rolesDelete: "roles.delete",
  permissionsRead: "permissions.read",
  permissionsCreate: "permissions.create",
  permissionsUpdate: "permissions.update",
  permissionsDelete: "permissions.delete",
  auditRead: "audit.read",
  billingRead: "billing.read",
  billingManage: "billing.manage",
} as const;

export type AuthorizationContext = {
  organizationId: string;
  membershipId: string;
  roleId: string;
  permissions: string[];
};
