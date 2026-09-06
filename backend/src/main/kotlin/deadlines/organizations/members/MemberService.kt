package deadlines.organizations.members

import deadlines.organizations.MembershipRole
import deadlines.organizations.OrganizationAccessDeniedException
import deadlines.organizations.OrganizationNotFoundException
import deadlines.organizations.OrganizationRepository
import deadlines.organizations.access.RoleNotFoundException
import deadlines.organizations.access.RoleRepository
import java.time.Clock
import java.util.UUID

interface MemberOperations {
    suspend fun list(userId: UUID): MemberListResponse

    suspend fun get(userId: UUID, membershipId: UUID): MemberResponse

    suspend fun updateRole(userId: UUID, membershipId: UUID, request: UpdateMemberRoleRequest): MemberResponse

    suspend fun remove(userId: UUID, membershipId: UUID)
}

class MemberService(
    private val organizations: OrganizationRepository,
    private val members: MemberRepository,
    private val roles: RoleRepository,
    private val clock: Clock = Clock.systemUTC(),
) : MemberOperations {
    override suspend fun list(userId: UUID): MemberListResponse {
        val organizationId = currentOrganization(userId).organization.id
        return MemberListResponse(members.list(organizationId).map(OrganizationMember::toResponse))
    }

    override suspend fun get(userId: UUID, membershipId: UUID): MemberResponse {
        val organizationId = currentOrganization(userId).organization.id
        return requireMember(organizationId, membershipId).toResponse()
    }

    override suspend fun updateRole(
        userId: UUID,
        membershipId: UUID,
        request: UpdateMemberRoleRequest,
    ): MemberResponse {
        val context = requireOwner(userId)
        val member = requireMember(context.organization.id, membershipId)
        if (member.role.key == OWNER_ROLE_KEY) throw OwnerMembershipImmutableException()

        val roleId = request.roleId.toUuid("roleId")
        val role = roles.findById(context.organization.id, roleId) ?: throw RoleNotFoundException()
        if (role.key == OWNER_ROLE_KEY) throw OwnerMembershipImmutableException()
        if (!members.updateRole(context.organization.id, membershipId, role.id)) throw MemberNotFoundException()
        return requireMember(context.organization.id, membershipId).toResponse()
    }

    override suspend fun remove(userId: UUID, membershipId: UUID) {
        val context = requireOwner(userId)
        val member = requireMember(context.organization.id, membershipId)
        if (member.role.key == OWNER_ROLE_KEY) throw OwnerMembershipImmutableException()
        if (!members.remove(context.organization.id, membershipId, clock.instant())) throw MemberNotFoundException()
    }

    private suspend fun currentOrganization(userId: UUID) =
        organizations.findCurrentByUser(userId) ?: throw OrganizationNotFoundException()

    private suspend fun requireOwner(userId: UUID) =
        currentOrganization(userId).also {
            if (it.membership.role != MembershipRole.OWNER) throw OrganizationAccessDeniedException()
        }

    private suspend fun requireMember(organizationId: UUID, membershipId: UUID) =
        members.findById(organizationId, membershipId) ?: throw MemberNotFoundException()

    private fun String.toUuid(field: String): UUID =
        runCatching { UUID.fromString(this) }.getOrElse {
            throw MemberValidationException(mapOf(field to "must be a valid UUID"))
        }

    private companion object {
        const val OWNER_ROLE_KEY = "owner"
    }
}
