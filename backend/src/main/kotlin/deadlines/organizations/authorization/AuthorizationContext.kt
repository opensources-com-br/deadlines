package deadlines.organizations.authorization

import java.util.UUID

data class AuthorizationContext(
    val userId: UUID,
    val organizationId: UUID,
    val membershipId: UUID,
    val roleId: UUID,
    val permissions: Set<String>,
) {
    fun hasPermission(permission: String): Boolean = permission in permissions
}
