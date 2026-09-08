package opensources.organizations.authorization

import opensources.organizations.OrganizationAccessDeniedException
import opensources.organizations.OrganizationNotFoundException
import java.util.UUID

interface AuthorizationOperations {
    suspend fun context(userId: UUID): AuthorizationContext

    suspend fun hasPermission(userId: UUID, permission: String): Boolean

    suspend fun requirePermission(userId: UUID, permission: String): AuthorizationContext

    suspend fun requireAnyPermission(userId: UUID, permissions: Set<String>): AuthorizationContext

    suspend fun requireAllPermissions(userId: UUID, permissions: Set<String>): AuthorizationContext
}

class AuthorizationService(
    private val repository: AuthorizationRepository,
) : AuthorizationOperations {
    override suspend fun context(userId: UUID): AuthorizationContext =
        repository.findByUserId(userId) ?: throw OrganizationNotFoundException()

    override suspend fun hasPermission(userId: UUID, permission: String): Boolean =
        repository.findByUserId(userId)?.hasPermission(permission) == true

    override suspend fun requirePermission(userId: UUID, permission: String): AuthorizationContext =
        context(userId).require { granted -> granted.hasPermission(permission) }

    override suspend fun requireAnyPermission(userId: UUID, permissions: Set<String>): AuthorizationContext =
        context(userId).require { granted -> permissions.isNotEmpty() && permissions.any(granted::hasPermission) }

    override suspend fun requireAllPermissions(userId: UUID, permissions: Set<String>): AuthorizationContext =
        context(userId).require { granted -> permissions.all(granted::hasPermission) }

    private fun AuthorizationContext.require(predicate: (AuthorizationContext) -> Boolean): AuthorizationContext =
        takeIf(predicate) ?: throw OrganizationAccessDeniedException()
}
