package opensources.identity.users

import opensources.identity.auth.InvalidCurrentPasswordException
import opensources.identity.auth.PasswordHasher
import opensources.organizations.MembershipRole
import opensources.organizations.MembershipStatus
import opensources.organizations.Organization
import opensources.organizations.OrganizationContext
import opensources.organizations.OrganizationMembership
import opensources.organizations.OrganizationRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountLifecycleServiceTest {
    private val now = Instant.parse("2026-09-07T18:00:00Z")
    private val userId = UUID.randomUUID()

    @Test
    fun `member can deactivate account after confirming password`() = runTest {
        val lifecycle = MemoryLifecycle()
        service(lifecycle = lifecycle).deactivate(userId, "correct-password")

        assertEquals(userId to now, lifecycle.deactivated)
    }

    @Test
    fun `member can delete account after confirming password`() = runTest {
        val lifecycle = MemoryLifecycle()
        service(lifecycle = lifecycle).delete(userId, "correct-password")

        assertEquals(userId to now, lifecycle.deleted)
    }

    @Test
    fun `owner must transfer ownership or delete organization first`() = runTest {
        val lifecycle = MemoryLifecycle()
        val service = service(role = MembershipRole.OWNER, lifecycle = lifecycle)

        assertFailsWith<AccountOwnerConflictException> { service.deactivate(userId, "correct-password") }
        assertFailsWith<AccountOwnerConflictException> { service.delete(userId, "correct-password") }
        assertEquals(null, lifecycle.deactivated)
        assertEquals(null, lifecycle.deleted)
    }

    @Test
    fun `rejects an invalid password`() = runTest {
        assertFailsWith<InvalidCurrentPasswordException> {
            service().delete(userId, "wrong-password")
        }
    }

    @Test
    fun `rejects lifecycle actions for an inactive account`() = runTest {
        assertFailsWith<AccountNotActiveException> {
            service(status = UserStatus.DISABLED).deactivate(userId, "correct-password")
        }
    }

    private fun service(
        role: MembershipRole = MembershipRole.MEMBER,
        status: UserStatus = UserStatus.ACTIVE,
        lifecycle: MemoryLifecycle = MemoryLifecycle(),
    ): AccountLifecycleService {
        val user = user(status)
        val users = LifecycleUsers(user)
        val credentials = LifecycleCredentials(UserCredentials(user, "correct-password"))
        return AccountLifecycleService(
            users,
            LifecycleOrganizations(context(role)),
            AccountPasswordVerifier(credentials, PlainPasswordHasher),
            lifecycle,
            Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    private fun user(status: UserStatus) = User(
        userId,
        "member@example.com",
        status,
        UserProfile("Member", "Example", null, null),
        now.minusSeconds(3600),
        now.minusSeconds(3600),
    )

    private fun context(role: MembershipRole): OrganizationContext {
        val organizationId = UUID.randomUUID()
        return OrganizationContext(
            Organization(organizationId, "Acme", "acme", userId, now, now),
            OrganizationMembership(UUID.randomUUID(), organizationId, userId, role, MembershipStatus.ACTIVE, now, null),
        )
    }
}

private class LifecycleUsers(private val user: User) : UserRepository {
    override suspend fun create(user: User) = user
    override suspend fun findById(id: UUID) = user.takeIf { it.id == id }
    override suspend fun findByEmail(email: String) = user.takeIf { it.email == email }
    override suspend fun list(offset: Long, limit: Int) = listOf(user)
    override suspend fun count() = 1L
    override suspend fun update(user: User) = user
    override suspend fun markEmailVerified(id: UUID, verifiedAt: Instant) = findById(id)
}

private class LifecycleCredentials(private val credentials: UserCredentials) : UserCredentialsRepository {
    override suspend fun create(user: User, passwordHash: String) = user
    override suspend fun findByEmail(email: String) = credentials.takeIf { it.user.email == email }
    override suspend fun findByUserId(userId: UUID) = credentials.takeIf { it.user.id == userId }
    override suspend fun updatePassword(userId: UUID, passwordHash: String, updatedAt: Instant) = false
}

private object PlainPasswordHasher : PasswordHasher {
    override suspend fun hash(password: String) = password
    override suspend fun verify(password: String, hash: String) = password == hash
}

private class LifecycleOrganizations(private val context: OrganizationContext) : OrganizationRepository {
    override suspend fun createWithOwner(context: OrganizationContext) = context
    override suspend fun findCurrentByUser(userId: UUID) = context.takeIf { it.membership.userId == userId }
    override suspend fun findRetainedByUser(userId: UUID) = findCurrentByUser(userId)
    override suspend fun update(organization: Organization) = organization
}

private class MemoryLifecycle : AccountLifecycleRepository {
    var deactivated: Pair<UUID, Instant>? = null
    var deleted: Pair<UUID, Instant>? = null

    override suspend fun deactivate(userId: UUID, disabledAt: Instant) = true.also {
        deactivated = userId to disabledAt
    }

    override suspend fun delete(userId: UUID, deletedAt: Instant) = true.also {
        deleted = userId to deletedAt
    }
}
