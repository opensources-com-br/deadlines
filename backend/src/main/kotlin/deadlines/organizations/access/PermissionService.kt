package deadlines.organizations.access

import deadlines.organizations.MembershipRole
import deadlines.organizations.OrganizationAccessDeniedException
import deadlines.organizations.OrganizationNotFoundException
import deadlines.organizations.OrganizationRepository
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
    private val organizations: OrganizationRepository,
    private val permissions: PermissionRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> UUID = UUID::randomUUID,
) : PermissionOperations {
    override suspend fun list(userId: UUID): PermissionListResponse {
        val context = organizations.findCurrentByUser(userId) ?: throw OrganizationNotFoundException()
        return PermissionListResponse(permissions.list(context.organization.id).map(Permission::toResponse))
    }

    override suspend fun get(userId: UUID, permissionId: UUID): PermissionResponse {
        val context = organizations.findCurrentByUser(userId) ?: throw OrganizationNotFoundException()
        return permissions.findById(context.organization.id, permissionId)?.toResponse()
            ?: throw PermissionNotFoundException()
    }

    override suspend fun create(userId: UUID, request: CreatePermissionRequest): PermissionResponse {
        val organizationId = requireOwner(userId)
        val now = clock.instant()
        return permissions.create(
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
    ): PermissionResponse {
        val organizationId = requireOwner(userId)
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
        return permissions.update(updated).toResponse()
    }

    override suspend fun delete(userId: UUID, permissionId: UUID) {
        val organizationId = requireOwner(userId)
        val current = permissions.findById(organizationId, permissionId) ?: throw PermissionNotFoundException()
        if (current.isSystem) throw SystemPermissionImmutableException()
        if (!permissions.delete(organizationId, permissionId)) throw PermissionNotFoundException()
    }

    private suspend fun requireOwner(userId: UUID): UUID {
        val context = organizations.findCurrentByUser(userId) ?: throw OrganizationNotFoundException()
        if (context.membership.role != MembershipRole.OWNER) throw OrganizationAccessDeniedException()
        return context.organization.id
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
