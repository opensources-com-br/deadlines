package deadlines.organizations

import deadlines.organizations.audits.withAuditActor

import java.time.Clock
import java.util.UUID

interface OrganizationOperations {
    suspend fun create(userId: UUID, request: CreateOrganizationRequest): OrganizationResponse

    suspend fun current(userId: UUID): OrganizationResponse

    suspend fun update(userId: UUID, request: UpdateOrganizationRequest): OrganizationResponse
}

class OrganizationService(
    private val repository: OrganizationRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> UUID = UUID::randomUUID,
) : OrganizationOperations {
    override suspend fun create(userId: UUID, request: CreateOrganizationRequest): OrganizationResponse = withAuditActor(userId) {
        if (repository.findCurrentByUser(userId) != null) throw ActiveMembershipAlreadyExistsException()
        val name = validateName(request.name)
        val slug = validateSlug(request.slug)
        val now = clock.instant()
        val organizationId = idGenerator()
        val context =
            OrganizationContext(
                organization =
                    Organization(
                        id = organizationId,
                        name = name,
                        slug = slug,
                        createdBy = userId,
                        createdAt = now,
                        updatedAt = now,
                    ),
                membership =
                    OrganizationMembership(
                        id = idGenerator(),
                        organizationId = organizationId,
                        userId = userId,
                        role = MembershipRole.OWNER,
                        status = MembershipStatus.ACTIVE,
                        joinedAt = now,
                        removedAt = null,
                    ),
            )
        return@withAuditActor repository.createWithOwner(context).toResponse()
    }

    override suspend fun current(userId: UUID): OrganizationResponse =
        repository.findCurrentByUser(userId)?.toResponse() ?: throw OrganizationNotFoundException()

    override suspend fun update(userId: UUID, request: UpdateOrganizationRequest): OrganizationResponse = withAuditActor(userId) {
        if (request.name == null && request.slug == null) {
            throw OrganizationValidationException(mapOf("body" to "must contain name or slug"))
        }
        val current = repository.findCurrentByUser(userId) ?: throw OrganizationNotFoundException()
        if (current.membership.role != MembershipRole.OWNER) throw OrganizationAccessDeniedException()

        val updated =
            current.organization.copy(
                name = request.name?.let(::validateName) ?: current.organization.name,
                slug = request.slug?.let(::validateSlug) ?: current.organization.slug,
                updatedAt = clock.instant(),
            )
        return@withAuditActor current.copy(organization = repository.update(updated)).toResponse()
    }

    private fun validateName(value: String): String {
        val normalized = value.trim()
        val violation =
            when {
                normalized.length < 2 -> "must contain at least 2 characters"
                normalized.length > 160 -> "must contain at most 160 characters"
                else -> null
            }
        if (violation != null) throw OrganizationValidationException(mapOf("name" to violation))
        return normalized
    }

    private fun validateSlug(value: String): String {
        val normalized = value.trim().lowercase()
        val violation =
            when {
                normalized.length < 2 -> "must contain at least 2 characters"
                normalized.length > 80 -> "must contain at most 80 characters"
                !SLUG_PATTERN.matches(normalized) -> "must use lowercase letters, numbers, and single hyphens"
                else -> null
            }
        if (violation != null) throw OrganizationValidationException(mapOf("slug" to violation))
        return normalized
    }

    private companion object {
        val SLUG_PATTERN = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
    }
}
