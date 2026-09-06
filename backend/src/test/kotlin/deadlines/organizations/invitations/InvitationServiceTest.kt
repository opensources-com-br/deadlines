package deadlines.organizations.invitations

import deadlines.config.EmailConfig
import deadlines.identity.email.EmailTokenGenerator
import deadlines.identity.email.EmailDeliveryException
import deadlines.identity.email.EmailService
import deadlines.identity.email.RecordingEmailService
import deadlines.identity.users.User
import deadlines.identity.users.UserProfile
import deadlines.identity.users.UserRepository
import deadlines.identity.users.UserStatus
import deadlines.organizations.ActiveMembershipAlreadyExistsException
import deadlines.organizations.Organization
import deadlines.organizations.OrganizationContext
import deadlines.organizations.OrganizationMembership
import deadlines.organizations.OrganizationRepository
import deadlines.organizations.access.MemoryRoleRepository
import deadlines.organizations.access.Role
import deadlines.organizations.members.MemberRepository
import deadlines.organizations.members.OrganizationMember
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InvitationServiceTest {
    private val now = Instant.parse("2026-09-06T18:00:00Z")
    private val ownerId = UUID.randomUUID()
    private val inviteeId = UUID.randomUUID()
    private val organizationId = UUID.randomUUID()
    private val memberRole = Role(UUID.randomUUID(), organizationId, "member", "Member", null, true, now, now)

    @Test
    fun `owner creates and emails a normalized invitation`() = runTest {
        val fixture = fixture()

        val response = fixture.service.create(ownerId, CreateInvitationRequest("  Invitee@Example.com ", memberRole.id.toString()))

        assertEquals("invitee@example.com", response.email)
        assertEquals("pending", response.status)
        assertEquals("invitee@example.com", fixture.recordedEmail.sentMessages.single().to)
        assertTrue(fixture.recordedEmail.sentMessages.single().text.contains("/invitations/accept?token=invite-token"))
    }

    @Test
    fun `verified user accepts an invitation and becomes a member`() = runTest {
        val fixture = fixture(listOf(user(inviteeId, "invitee@example.com")))
        fixture.service.create(ownerId, CreateInvitationRequest("invitee@example.com", memberRole.id.toString()))

        val member = fixture.service.accept(inviteeId, "invite-token")

        assertEquals(inviteeId.toString(), member.userId)
        assertEquals("member", member.role.key)
    }

    @Test
    fun `invitation requires the matching account email`() = runTest {
        val fixture = fixture(listOf(user(inviteeId, "different@example.com")))
        fixture.service.create(ownerId, CreateInvitationRequest("invitee@example.com", memberRole.id.toString()))

        assertFailsWith<InvitationEmailMismatchException> {
            fixture.service.accept(inviteeId, "invite-token")
        }
    }

    @Test
    fun `user can receive invitations but cannot join a second organization`() = runTest {
        val otherContext = organizationContext(inviteeId, UUID.randomUUID(), owner = false)
        val fixture = fixture(listOf(user(inviteeId, "invitee@example.com")), mapOf(inviteeId to otherContext))
        fixture.service.create(ownerId, CreateInvitationRequest("invitee@example.com", memberRole.id.toString()))

        assertFailsWith<ActiveMembershipAlreadyExistsException> {
            fixture.service.accept(inviteeId, "invite-token")
        }
    }

    @Test
    fun `owner renews and revokes pending invitations`() = runTest {
        val fixture = fixture()
        val invitation = fixture.service.create(ownerId, CreateInvitationRequest("invitee@example.com", memberRole.id.toString()))

        val renewed = fixture.service.resend(ownerId, UUID.fromString(invitation.id))
        assertEquals("pending", renewed.status)
        assertEquals(2, fixture.recordedEmail.sentMessages.size)

        fixture.service.revoke(ownerId, UUID.fromString(invitation.id))
        assertEquals("revoked", fixture.service.get(ownerId, UUID.fromString(invitation.id)).status)
    }

    @Test
    fun `failed invitation delivery revokes the newly created invitation`() = runTest {
        val fixture = fixture(email = FailingEmailService())

        assertFailsWith<EmailDeliveryException> {
            fixture.service.create(ownerId, CreateInvitationRequest("invitee@example.com", memberRole.id.toString()))
        }

        assertEquals("revoked", fixture.service.list(ownerId).data.single().status)
    }

    private fun fixture(
        extraUsers: List<User> = emptyList(),
        extraContexts: Map<UUID, OrganizationContext> = emptyMap(),
        email: EmailService = RecordingEmailService(),
    ): Fixture {
        val ownerContext = organizationContext(ownerId, organizationId, owner = true)
        val organizations = MemoryOrganizations(mutableMapOf(ownerId to ownerContext).apply { putAll(extraContexts) })
        val users = MemoryUsers(listOf(user(ownerId, "owner@example.com")) + extraUsers)
        val members = MemoryMembers(memberRole)
        val invitations = MemoryInvitations(members, users)
        val service =
            InvitationService(
                organizations,
                invitations,
                MemoryRoleRepository(listOf(memberRole)),
                members,
                users,
                email,
                EmailConfig("no-reply@example.com", "https://app.example.com", 3600, 3600, 604800),
                FixedInvitationTokenGenerator(),
                Clock.fixed(now, ZoneOffset.UTC),
                UUID::randomUUID,
            )
        return Fixture(service, email)
    }

    private fun user(id: UUID, email: String) =
        User(id, email, UserStatus.ACTIVE, UserProfile("Test", "User", null, null), now, now, now)

    private fun organizationContext(userId: UUID, id: UUID, owner: Boolean): OrganizationContext =
        deadlines.organizations.access.accessContext(
            userId,
            id,
            if (owner) deadlines.organizations.MembershipRole.OWNER else deadlines.organizations.MembershipRole.MEMBER,
        )

    private data class Fixture(val service: InvitationService, val email: EmailService) {
        val recordedEmail: RecordingEmailService
            get() = email as RecordingEmailService
    }
}

private class FailingEmailService : EmailService {
    override suspend fun send(message: deadlines.identity.email.EmailMessage): Nothing = throw EmailDeliveryException()
}

private class FixedInvitationTokenGenerator : EmailTokenGenerator {
    override fun generate() = "invite-token"
    override fun hash(token: String) = token.padEnd(64, '0').take(64)
}

private class MemoryOrganizations(
    private val contexts: MutableMap<UUID, OrganizationContext>,
) : OrganizationRepository {
    override suspend fun createWithOwner(context: OrganizationContext) = context.also { contexts[it.membership.userId] = it }
    override suspend fun findCurrentByUser(userId: UUID) = contexts[userId]
    override suspend fun update(organization: Organization) = organization
}

private class MemoryUsers(initial: List<User>) : UserRepository {
    private val values = initial.toMutableList()
    override suspend fun create(user: User) = user.also(values::add)
    override suspend fun findById(id: UUID) = values.firstOrNull { it.id == id }
    override suspend fun findByEmail(email: String) = values.firstOrNull { it.email.equals(email, true) }
    override suspend fun list(offset: Long, limit: Int) = values.drop(offset.toInt()).take(limit)
    override suspend fun count() = values.size.toLong()
    override suspend fun update(user: User) = user
    override suspend fun markEmailVerified(id: UUID, verifiedAt: Instant) = findById(id)
}

private class MemoryMembers(
    private val role: Role,
) : MemberRepository {
    private val values = mutableListOf<OrganizationMember>()

    fun add(invitation: OrganizationInvitation, user: User, membershipId: UUID, joinedAt: Instant) {
        values += OrganizationMember(membershipId, invitation.organizationId, user.id, user.email, user.profile.firstName, user.profile.lastName, role, joinedAt)
    }

    override suspend fun list(organizationId: UUID) = values.filter { it.organizationId == organizationId }
    override suspend fun findById(organizationId: UUID, membershipId: UUID) =
        values.firstOrNull { it.organizationId == organizationId && it.membershipId == membershipId }
    override suspend fun findByUserId(organizationId: UUID, userId: UUID) =
        values.firstOrNull { it.organizationId == organizationId && it.userId == userId }
    override suspend fun updateRole(organizationId: UUID, membershipId: UUID, roleId: UUID) = false
    override suspend fun remove(organizationId: UUID, membershipId: UUID, removedAt: Instant) = false
}

private class MemoryInvitations(
    private val members: MemoryMembers,
    private val users: MemoryUsers,
) : InvitationRepository {
    private val values = mutableListOf<OrganizationInvitation>()

    override suspend fun list(organizationId: UUID, now: Instant) = values.filter { it.organizationId == organizationId }
    override suspend fun findById(organizationId: UUID, invitationId: UUID, now: Instant) =
        values.firstOrNull { it.organizationId == organizationId && it.id == invitationId }
    override suspend fun findByTokenHash(tokenHash: String, now: Instant) = values.firstOrNull { it.tokenHash == tokenHash }
    override suspend fun create(invitation: OrganizationInvitation) = invitation.also(values::add)

    override suspend fun renew(organizationId: UUID, invitationId: UUID, tokenHash: String, expiresAt: Instant, now: Instant): Boolean {
        val index = values.indexOfFirst { it.organizationId == organizationId && it.id == invitationId }
        if (index < 0) return false
        values[index] = values[index].copy(tokenHash = tokenHash, expiresAt = expiresAt, updatedAt = now, status = InvitationStatus.PENDING)
        return true
    }

    override suspend fun revoke(organizationId: UUID, invitationId: UUID, now: Instant): Boolean {
        val index = values.indexOfFirst { it.organizationId == organizationId && it.id == invitationId && it.status == InvitationStatus.PENDING }
        if (index < 0) return false
        values[index] = values[index].copy(status = InvitationStatus.REVOKED, revokedAt = now, updatedAt = now)
        return true
    }

    override suspend fun accept(invitation: OrganizationInvitation, userId: UUID, membershipId: UUID, now: Instant): Boolean {
        val index = values.indexOfFirst { it.id == invitation.id && it.status == InvitationStatus.PENDING }
        val user = users.findById(userId) ?: return false
        if (index < 0) return false
        values[index] = values[index].copy(status = InvitationStatus.ACCEPTED, acceptedBy = userId, acceptedAt = now, updatedAt = now)
        members.add(invitation, user, membershipId, now)
        return true
    }
}
