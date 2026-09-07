package deadlines.organizations.access

import deadlines.organizations.audits.withAuditActor

import deadlines.organizations.authorization.AuthorizationOperations
import deadlines.organizations.authorization.PlatformPermission
import java.time.Clock
import java.util.UUID

interface RoleOperations {
    suspend fun list(userId: UUID): RoleListResponse

    suspend fun get(userId: UUID, roleId: UUID): RoleResponse

    suspend fun create(userId: UUID, request: CreateRoleRequest): RoleResponse

    suspend fun update(userId: UUID, roleId: UUID, request: UpdateRoleRequest): RoleResponse

    suspend fun delete(userId: UUID, roleId: UUID)

    suspend fun listPermissions(userId: UUID, roleId: UUID): PermissionListResponse

    suspend fun replacePermissions(
        userId: UUID,
        roleId: UUID,
        request: ReplaceRolePermissionsRequest,
    ): PermissionListResponse
}

class RoleService(
    private val authorization: AuthorizationOperations,
    private val roles: RoleRepository,
    private val permissions: PermissionRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> UUID = UUID::randomUUID,
) : RoleOperations {
    override suspend fun list(userId: UUID): RoleListResponse {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.ROLES_READ).organizationId
        return RoleListResponse(roles.list(organizationId).map(Role::toResponse))
    }

    override suspend fun get(userId: UUID, roleId: UUID): RoleResponse =
        requireRole(authorization.requirePermission(userId, PlatformPermission.ROLES_READ).organizationId, roleId).toResponse()

    override suspend fun create(userId: UUID, request: CreateRoleRequest): RoleResponse = withAuditActor(userId) {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.ROLES_CREATE).organizationId
        val now = clock.instant()
        return@withAuditActor roles.create(
            Role(
                id = idGenerator(),
                organizationId = organizationId,
                key = validateKey(request.key),
                name = validateName(request.name),
                description = validateDescription(request.description),
                isSystem = false,
                createdAt = now,
                updatedAt = now,
            ),
        ).toResponse()
    }

    override suspend fun update(userId: UUID, roleId: UUID, request: UpdateRoleRequest): RoleResponse = withAuditActor(userId) {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.ROLES_UPDATE).organizationId
        if (request.key == null && request.name == null && request.description == null) {
            throw AccessValidationException(mapOf("body" to "must contain key, name, or description"))
        }
        val current = requireRole(organizationId, roleId)
        if (current.isSystem) throw SystemRoleImmutableException()
        val updated =
            current.copy(
                key = request.key?.let(::validateKey) ?: current.key,
                name = request.name?.let(::validateName) ?: current.name,
                description = if (request.description != null) validateDescription(request.description) else current.description,
                updatedAt = clock.instant(),
            )
        return@withAuditActor roles.update(updated).toResponse()
    }

    override suspend fun delete(userId: UUID, roleId: UUID) = withAuditActor(userId) {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.ROLES_DELETE).organizationId
        val current = requireRole(organizationId, roleId)
        if (current.isSystem) throw SystemRoleImmutableException()
        if (!roles.delete(organizationId, roleId)) throw RoleNotFoundException()
    }

    override suspend fun listPermissions(userId: UUID, roleId: UUID): PermissionListResponse {
        val organizationId = authorization.requireAllPermissions(
            userId,
            setOf(PlatformPermission.ROLES_READ, PlatformPermission.PERMISSIONS_READ),
        ).organizationId
        requireRole(organizationId, roleId)
        return PermissionListResponse(roles.listPermissions(roleId).map(Permission::toResponse))
    }

    override suspend fun replacePermissions(
        userId: UUID,
        roleId: UUID,
        request: ReplaceRolePermissionsRequest,
    ): PermissionListResponse = withAuditActor(userId) {
        val organizationId = authorization.requireAllPermissions(
            userId,
            setOf(PlatformPermission.ROLES_UPDATE, PlatformPermission.PERMISSIONS_READ),
        ).organizationId
        val role = requireRole(organizationId, roleId)
        if (role.key == "owner") throw OwnerPermissionsImmutableException()
        val permissionIds = parsePermissionIds(request.permissionIds)
        val available = permissions.list(organizationId).associateBy(Permission::id)
        if (permissionIds.any { it !in available }) {
            throw AccessValidationException(mapOf("permissionIds" to "contains an unavailable permission"))
        }
        roles.replacePermissions(roleId, permissionIds)
        return@withAuditActor PermissionListResponse(roles.listPermissions(roleId).map(Permission::toResponse))
    }

    private suspend fun requireRole(organizationId: UUID, roleId: UUID): Role =
        roles.findById(organizationId, roleId) ?: throw RoleNotFoundException()

    private fun parsePermissionIds(values: List<String>): List<UUID> =
        values.distinct().map { value ->
            runCatching { UUID.fromString(value) }
                .getOrElse {
                    throw AccessValidationException(mapOf("permissionIds" to "must contain valid UUIDs"))
                }
        }

    private fun validateKey(value: String): String {
        val normalized = value.trim().lowercase()
        val violation =
            when {
                normalized.length !in 2..80 -> "must contain between 2 and 80 characters"
                !KEY_PATTERN.matches(normalized) -> "must use letters, numbers, underscores, or hyphens"
                else -> null
            }
        if (violation != null) throw AccessValidationException(mapOf("key" to violation))
        return normalized
    }

    private fun validateName(value: String): String {
        val normalized = value.trim()
        if (normalized.length !in 2..120) {
            throw AccessValidationException(mapOf("name" to "must contain between 2 and 120 characters"))
        }
        return normalized
    }

    private fun validateDescription(value: String?): String? {
        val normalized = value?.trim()?.ifEmpty { null }
        if (normalized != null && normalized.length > 500) {
            throw AccessValidationException(mapOf("description" to "must contain at most 500 characters"))
        }
        return normalized
    }

    private companion object {
        val KEY_PATTERN = Regex("^[a-z][a-z0-9]*(?:[_-][a-z0-9]+)*$")
    }
}
