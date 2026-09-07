package deadlines.organizations.authorization

object PlatformPermission {
    const val ORGANIZATION_READ = "organization.read"
    const val ORGANIZATION_UPDATE = "organization.update"
    const val MEMBERS_READ = "members.read"
    const val MEMBERS_INVITE = "members.invite"
    const val MEMBERS_UPDATE = "members.update"
    const val MEMBERS_REMOVE = "members.remove"
    const val ROLES_READ = "roles.read"
    const val ROLES_CREATE = "roles.create"
    const val ROLES_UPDATE = "roles.update"
    const val ROLES_DELETE = "roles.delete"
    const val PERMISSIONS_READ = "permissions.read"
    const val PERMISSIONS_CREATE = "permissions.create"
    const val PERMISSIONS_UPDATE = "permissions.update"
    const val PERMISSIONS_DELETE = "permissions.delete"
    const val AUDIT_READ = "audit.read"
    const val BILLING_READ = "billing.read"
    const val BILLING_MANAGE = "billing.manage"
}
