package deadlines.organizations.members

import deadlines.organizations.MembershipRole
import deadlines.organizations.OrganizationAccessDeniedException
import deadlines.organizations.access.MemoryRoleRepository
import deadlines.organizations.access.Role
import deadlines.organizations.access.TestOrganizationRepository
import deadlines.organizations.access.accessContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `owner assigns an organization role to a member`() = runTest {
        val repository = MemoryMemberRepository(listOf(owner, teammate), listOf(ownerRole, memberRole, managerRole))
        val service = service(repository)

        val updated = service.updateRole(ownerId, teammate.membershipId, UpdateMemberRoleRequest(managerRole.id.toString()))

        assertEquals("manager", updated.role.key)
    }

    @Test
    fun `owner membership cannot be reassigned or removed`() = runTest {
        val service = service()

        assertFailsWith<OwnerMembershipImmutableException> {
            service.updateRole(ownerId, owner.membershipId, UpdateMemberRoleRequest(memberRole.id.toString()))
        }
        assertFailsWith<OwnerMembershipImmutableException> {
            service.remove(ownerId, owner.membershipId)
        }
    }

    @Test
    fun `regular member cannot manage organization members`() = runTest {
        val organizations = TestOrganizationRepository(accessContext(teammate.userId, organizationId, MembershipRole.MEMBER))
        val service = MemberService(organizations, memberRepository(), roles(), fixedClock())

        assertFailsWith<OrganizationAccessDeniedException> {
            service.remove(teammate.userId, owner.membershipId)
        }
    }

    private fun service(repository: MemoryMemberRepository = memberRepository()) =
        MemberService(
            TestOrganizationRepository(accessContext(ownerId, organizationId)),
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

    override suspend fun list(organizationId: UUID) = values.filter { it.organizationId == organizationId }

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
        values.removeIf { it.organizationId == organizationId && it.membershipId == membershipId }
}
