package opensources.organizations.access

import opensources.organizations.audits.withAuditActor

import opensources.organizations.authorization.AuthorizationOperations
import opensources.organizations.authorization.PlatformPermission
import java.time.Clock
import java.util.UUID

interface PermissionOperations {
    suspend fun list(userId: UUID): PermissionListResponse

    suspend fun get(userId: UUID, permissionId: UUID): PermissionResponse

    suspend fun create(userId: UUID, request: CreatePermissionRequest): PermissionResponse

    suspend fun update(userId: UUID, permissionId: UUID, request: UpdatePermissionRequest): PermissionResponse

    suspend fun delete(userId: UUID, permissionId: UUID)
}

class PermissionService(
    private val authorization: AuthorizationOperations,
    private val permissions: PermissionRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> UUID = UUID::randomUUID,
) : PermissionOperations {
    override suspend fun list(userId: UUID): PermissionListResponse {
        val context = authorization.requirePermission(userId, PlatformPermission.PERMISSIONS_READ)
        return PermissionListResponse(permissions.list(context.organizationId).map(Permission::toResponse))
    }

    override suspend fun get(userId: UUID, permissionId: UUID): PermissionResponse {
        val context = authorization.requirePermission(userId, PlatformPermission.PERMISSIONS_READ)
        return permissions.findById(context.organizationId, permissionId)?.toResponse()
            ?: throw PermissionNotFoundException()
    }

    override suspend fun create(userId: UUID, request: CreatePermissionRequest): PermissionResponse = withAuditActor(userId) {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.PERMISSIONS_CREATE).organizationId
        val now = clock.instant()
        return@withAuditActor permissions.create(
            Permission(
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

    override suspend fun update(
        userId: UUID,
        permissionId: UUID,
        request: UpdatePermissionRequest,
    ): PermissionResponse = withAuditActor(userId) {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.PERMISSIONS_UPDATE).organizationId
        if (request.key == null && request.name == null && request.description == null) {
            throw AccessValidationException(mapOf("body" to "must contain key, name, or description"))
        }
        val current = permissions.findById(organizationId, permissionId) ?: throw PermissionNotFoundException()
        if (current.isSystem) throw SystemPermissionImmutableException()
        val updated =
            current.copy(
                key = request.key?.let(::validateKey) ?: current.key,
                name = request.name?.let(::validateName) ?: current.name,
                description = if (request.description != null) validateDescription(request.description) else current.description,
                updatedAt = clock.instant(),
            )
        return@withAuditActor permissions.update(updated).toResponse()
    }

    override suspend fun delete(userId: UUID, permissionId: UUID) = withAuditActor(userId) {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.PERMISSIONS_DELETE).organizationId
        val current = permissions.findById(organizationId, permissionId) ?: throw PermissionNotFoundException()
        if (current.isSystem) throw SystemPermissionImmutableException()
        if (!permissions.delete(organizationId, permissionId)) throw PermissionNotFoundException()
    }

    private fun validateKey(value: String): String {
        val normalized = value.trim().lowercase()
        val violation =
            when {
                normalized.length !in 2..100 -> "must contain between 2 and 100 characters"
                !KEY_PATTERN.matches(normalized) -> "must use letters, numbers, dots, underscores, or hyphens"
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
        val KEY_PATTERN = Regex("^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$")
    }
}
