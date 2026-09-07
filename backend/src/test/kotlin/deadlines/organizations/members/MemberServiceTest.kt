package deadlines.organizations.members

import deadlines.organizations.OrganizationAccessDeniedException
import deadlines.organizations.MembershipStatus
import deadlines.organizations.access.MemoryRoleRepository
import deadlines.organizations.access.Role
import deadlines.organizations.authorization.PlatformPermission
import deadlines.organizations.authorization.testAuthorization
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class MemberServiceTest {
    private val now = Instant.parse("2026-09-06T18:00:00Z")
    private val ownerId = UUID.randomUUID()
    private val organizationId = UUID.randomUUID()
    private val ownerRole = role("owner", true)
    private val memberRole = role("member", true)
    private val managerRole = role("manager", false)
    private val owner = member(ownerId, ownerRole)
    private val teammate = member(UUID.randomUUID(), memberRole)

    @Test
    fun `lists and reads active members in the current organization`() = runTest {
        val service = service()

        assertEquals(2, service.list(ownerId).data.size)
        assertEquals(teammate.email, service.get(ownerId, teammate.membershipId).email)
    }

    @Test
    fun `authorized user assigns an organization role to a member`() = runTest {
        val repository = MemoryMemberRepository(listOf(owner, teammate), listOf(ownerRole, memberRole, managerRole))
        val service = service(repository)

        val updated = service.updateRole(ownerId, teammate.membershipId, UpdateMemberRoleRequest(managerRole.id.toString()))

        assertEquals("manager", updated.role.key)
    }

    @Test
    fun `owner membership remains immutable`() = runTest {
        val service = service()

        assertFailsWith<OwnerMembershipImmutableException> {
            service.updateRole(ownerId, owner.membershipId, UpdateMemberRoleRequest(memberRole.id.toString()))
        }
        assertFailsWith<OwnerMembershipImmutableException> {
            service.remove(ownerId, owner.membershipId)
        }
    }

    @Test
    fun `member removal requires permission`() = runTest {
        val service = MemberService(
            testAuthorization(teammate.userId, organizationId, PlatformPermission.MEMBERS_READ),
            memberRepository(),
            roles(),
            fixedClock(),
        )

        assertFailsWith<OrganizationAccessDeniedException> {
            service.remove(teammate.userId, owner.membershipId)
        }
    }

    @Test
    fun `authorized user suspends and reactivates a member`() = runTest {
        val repository = memberRepository()
        val service = service(repository)

        assertEquals("suspended", service.suspend(ownerId, teammate.membershipId).status)
        assertEquals("active", service.reactivate(ownerId, teammate.membershipId).status)
    }

    @Test
    fun `member leaves while owner must transfer first`() = runTest {
        val repository = memberRepository()
        val memberService = MemberService(
            testAuthorization(teammate.userId, organizationId),
            repository,
            roles(),
            fixedClock(),
        )

        memberService.leave(teammate.userId)
        assertEquals(MembershipStatus.REMOVED, repository.value(teammate.membershipId)?.status)
        assertFailsWith<OwnerCannotLeaveException> { service(repository).leave(ownerId) }
    }

    @Test
    fun `owner transfers ownership and receives selected role`() = runTest {
        val repository = memberRepository()

        val updated = service(repository).transferOwnership(
            ownerId,
            teammate.membershipId,
            TransferOwnershipRequest(managerRole.id.toString()),
        )

        assertEquals("owner", updated.role.key)
        assertEquals("manager", repository.value(owner.membershipId)?.role?.key)
        assertNotEquals("owner", repository.value(owner.membershipId)?.role?.key)
    }

    private fun service(repository: MemoryMemberRepository = memberRepository()) =
        MemberService(
            testAuthorization(
                ownerId,
                organizationId,
                PlatformPermission.MEMBERS_READ,
                PlatformPermission.MEMBERS_UPDATE,
                PlatformPermission.MEMBERS_REMOVE,
            ),
            repository,
            roles(),
            fixedClock(),
        )

    private fun roles() = MemoryRoleRepository(listOf(ownerRole, memberRole, managerRole))

    private fun memberRepository() =
        MemoryMemberRepository(listOf(owner, teammate), listOf(ownerRole, memberRole, managerRole))

    private fun fixedClock() = Clock.fixed(now, ZoneOffset.UTC)

    private fun role(key: String, system: Boolean) =
        Role(UUID.randomUUID(), organizationId, key, key.replaceFirstChar(Char::uppercase), null, system, now, now)

    private fun member(userId: UUID, role: Role) =
        OrganizationMember(
            UUID.randomUUID(),
            organizationId,
            userId,
            "$userId@example.com",
            "Test",
            "Member",
            role,
            now,
        )
}

private class MemoryMemberRepository(
    initial: List<OrganizationMember>,
    roles: List<Role>,
) : MemberRepository {
    private val values = initial.toMutableList()
    private val rolesById = roles.associateBy(Role::id)

    override suspend fun list(organizationId: UUID) = values.filter {
        it.organizationId == organizationId && it.status != MembershipStatus.REMOVED
    }

    override suspend fun findById(organizationId: UUID, membershipId: UUID) =
        values.firstOrNull { it.organizationId == organizationId && it.membershipId == membershipId }

    override suspend fun findByUserId(organizationId: UUID, userId: UUID) =
        values.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override suspend fun updateRole(organizationId: UUID, membershipId: UUID, roleId: UUID): Boolean {
        val index = values.indexOfFirst { it.organizationId == organizationId && it.membershipId == membershipId }
        if (index < 0) return false
        val role = rolesById[roleId] ?: return false
        values[index] = values[index].copy(role = role)
        return true
    }

    override suspend fun remove(organizationId: UUID, membershipId: UUID, removedAt: Instant): Boolean =
        updateStatus(organizationId, membershipId, MembershipStatus.REMOVED)

    override suspend fun suspend(organizationId: UUID, membershipId: UUID): Boolean =
        updateStatus(organizationId, membershipId, MembershipStatus.SUSPENDED, MembershipStatus.ACTIVE)

    override suspend fun reactivate(organizationId: UUID, membershipId: UUID): Boolean =
        updateStatus(organizationId, membershipId, MembershipStatus.ACTIVE, MembershipStatus.SUSPENDED)

    override suspend fun transferOwnership(
        organizationId: UUID,
        currentOwnerMembershipId: UUID,
        nextOwnerMembershipId: UUID,
        ownerRoleId: UUID,
        previousOwnerRoleId: UUID,
    ): Boolean {
        val currentIndex = values.indexOfFirst { it.organizationId == organizationId && it.membershipId == currentOwnerMembershipId }
        val nextIndex = values.indexOfFirst { it.organizationId == organizationId && it.membershipId == nextOwnerMembershipId }
        val ownerRole = rolesById[ownerRoleId] ?: return false
        val previousRole = rolesById[previousOwnerRoleId] ?: return false
        if (currentIndex < 0 || nextIndex < 0 || values[nextIndex].status != MembershipStatus.ACTIVE) return false
        values[currentIndex] = values[currentIndex].copy(role = previousRole)
        values[nextIndex] = values[nextIndex].copy(role = ownerRole)
        return true
    }

    fun value(membershipId: UUID) = values.firstOrNull { it.membershipId == membershipId }

    private fun updateStatus(
        organizationId: UUID,
        membershipId: UUID,
        nextStatus: MembershipStatus,
        requiredStatus: MembershipStatus? = null,
    ): Boolean {
        val index = values.indexOfFirst {
            it.organizationId == organizationId && it.membershipId == membershipId &&
                (requiredStatus == null || it.status == requiredStatus)
        }
        if (index < 0) return false
        values[index] = values[index].copy(status = nextStatus)
        return true
    }
}
