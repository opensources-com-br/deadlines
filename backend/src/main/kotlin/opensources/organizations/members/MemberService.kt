package opensources.organizations.members

import opensources.organizations.audits.withAuditActor
import opensources.organizations.MembershipStatus

import opensources.organizations.access.RoleNotFoundException
import opensources.organizations.access.RoleRepository
import opensources.organizations.authorization.AuthorizationOperations
import opensources.organizations.authorization.PlatformPermission
import java.time.Clock
import java.util.UUID

interface MemberOperations {
    suspend fun list(userId: UUID): MemberListResponse

    suspend fun get(userId: UUID, membershipId: UUID): MemberResponse

    suspend fun updateRole(userId: UUID, membershipId: UUID, request: UpdateMemberRoleRequest): MemberResponse

    suspend fun suspend(userId: UUID, membershipId: UUID): MemberResponse = throw UnsupportedOperationException()

    suspend fun reactivate(userId: UUID, membershipId: UUID): MemberResponse = throw UnsupportedOperationException()

    suspend fun leave(userId: UUID): Unit = throw UnsupportedOperationException()

    suspend fun transferOwnership(
        userId: UUID,
        nextOwnerMembershipId: UUID,
        request: TransferOwnershipRequest,
    ): MemberResponse = throw UnsupportedOperationException()

    suspend fun remove(userId: UUID, membershipId: UUID)
}

class MemberService(
    private val authorization: AuthorizationOperations,
    private val members: MemberRepository,
    private val roles: RoleRepository,
    private val clock: Clock = Clock.systemUTC(),
) : MemberOperations {
    override suspend fun list(userId: UUID): MemberListResponse {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.MEMBERS_READ).organizationId
        return MemberListResponse(members.list(organizationId).map(OrganizationMember::toResponse))
    }

    override suspend fun get(userId: UUID, membershipId: UUID): MemberResponse {
        val organizationId = authorization.requirePermission(userId, PlatformPermission.MEMBERS_READ).organizationId
        return requireMember(organizationId, membershipId).toResponse()
    }

    override suspend fun updateRole(
        userId: UUID,
        membershipId: UUID,
        request: UpdateMemberRoleRequest,
    ): MemberResponse = withAuditActor(userId) {
        val context = authorization.requirePermission(userId, PlatformPermission.MEMBERS_UPDATE)
        val member = requireMember(context.organizationId, membershipId)
        if (member.role.key == OWNER_ROLE_KEY) throw OwnerMembershipImmutableException()

        val roleId = request.roleId.toUuid("roleId")
        val role = roles.findById(context.organizationId, roleId) ?: throw RoleNotFoundException()
        if (role.key == OWNER_ROLE_KEY) throw OwnerMembershipImmutableException()
        if (!members.updateRole(context.organizationId, membershipId, role.id)) throw MemberNotFoundException()
        return@withAuditActor requireMember(context.organizationId, membershipId).toResponse()
    }

    override suspend fun remove(userId: UUID, membershipId: UUID) = withAuditActor(userId) {
        val context = authorization.requirePermission(userId, PlatformPermission.MEMBERS_REMOVE)
        val member = requireMember(context.organizationId, membershipId)
        if (member.role.key == OWNER_ROLE_KEY) throw OwnerMembershipImmutableException()
        if (!members.remove(context.organizationId, membershipId, clock.instant())) throw MemberNotFoundException()
    }

    override suspend fun suspend(userId: UUID, membershipId: UUID): MemberResponse = withAuditActor(userId) {
        val context = authorization.requirePermission(userId, PlatformPermission.MEMBERS_UPDATE)
        val member = requireMember(context.organizationId, membershipId)
        if (member.role.key == OWNER_ROLE_KEY) throw OwnerMembershipImmutableException()
        if (!members.suspend(context.organizationId, membershipId)) throw MembershipStateConflictException()
        return@withAuditActor requireMember(context.organizationId, membershipId).toResponse()
    }

    override suspend fun reactivate(userId: UUID, membershipId: UUID): MemberResponse = withAuditActor(userId) {
        val context = authorization.requirePermission(userId, PlatformPermission.MEMBERS_UPDATE)
        val member = requireMember(context.organizationId, membershipId)
        if (member.role.key == OWNER_ROLE_KEY) throw OwnerMembershipImmutableException()
        if (!members.reactivate(context.organizationId, membershipId)) throw MembershipStateConflictException()
        return@withAuditActor requireMember(context.organizationId, membershipId).toResponse()
    }

    override suspend fun leave(userId: UUID) = withAuditActor(userId) {
        val context = authorization.context(userId)
        val member = requireMember(context.organizationId, context.membershipId)
        if (member.role.key == OWNER_ROLE_KEY) throw OwnerCannotLeaveException()
        if (!members.remove(context.organizationId, context.membershipId, clock.instant())) {
            throw MembershipStateConflictException()
        }
    }

    override suspend fun transferOwnership(
        userId: UUID,
        nextOwnerMembershipId: UUID,
        request: TransferOwnershipRequest,
    ): MemberResponse = withAuditActor(userId) {
        val context = authorization.requirePermission(userId, PlatformPermission.MEMBERS_UPDATE)
        val currentOwner = requireMember(context.organizationId, context.membershipId)
        if (currentOwner.role.key != OWNER_ROLE_KEY) throw OwnershipTransferDeniedException()

        val nextOwner = requireMember(context.organizationId, nextOwnerMembershipId)
        if (nextOwner.status != MembershipStatus.ACTIVE || nextOwner.membershipId == currentOwner.membershipId) {
            throw OwnershipTransferTargetException()
        }

        val previousOwnerRoleId = request.previousOwnerRoleId.toUuid("previousOwnerRoleId")
        val previousOwnerRole = roles.findById(context.organizationId, previousOwnerRoleId) ?: throw RoleNotFoundException()
        if (previousOwnerRole.key == OWNER_ROLE_KEY) throw OwnershipTransferRoleException()
        val ownerRole = roles.list(context.organizationId).singleOrNull { it.key == OWNER_ROLE_KEY }
            ?: throw OwnershipInvariantException()

        if (!members.transferOwnership(
                context.organizationId,
                currentOwner.membershipId,
                nextOwner.membershipId,
                ownerRole.id,
                previousOwnerRole.id,
            )
        ) throw OwnershipTransferTargetException()
        return@withAuditActor requireMember(context.organizationId, nextOwnerMembershipId).toResponse()
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
