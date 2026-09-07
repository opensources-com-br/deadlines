package deadlines.organizations

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

class OrganizationServiceTest {
    private val now = Instant.parse("2026-09-06T18:00:00Z")
    private val userId = UUID.randomUUID()
    private val organizationId = UUID.randomUUID()
    private val membershipId = UUID.randomUUID()

    @Test
    fun `creates an organization with the authenticated user as owner`() =
        runTest {
            val repository = MemoryOrganizationRepository()
            val ids = ArrayDeque(listOf(organizationId, membershipId))
            val service = OrganizationService(
                repository,
                testAuthorization(userId, organizationId),
                fixedClock(),
                ids::removeFirst,
            )

            val response = service.create(userId, CreateOrganizationRequest("  Acme Inc  ", "  ACME-INC  "))

            assertEquals(organizationId.toString(), response.id)
            assertEquals("Acme Inc", response.name)
            assertEquals("acme-inc", response.slug)
            assertEquals("owner", response.role)
            assertEquals(userId, repository.context?.membership?.userId)
        }

    @Test
    fun `rejects creation when the user already has an active membership`() =
        runTest {
            val repository = MemoryOrganizationRepository(context(userId, MembershipRole.OWNER))
            val service = OrganizationService(repository, testAuthorization(userId, organizationId), fixedClock())

            assertFailsWith<ActiveMembershipAlreadyExistsException> {
                service.create(userId, CreateOrganizationRequest("Another", "another"))
            }
        }

    @Test
    fun `returns current organization and reports missing onboarding`() =
        runTest {
            val repository = MemoryOrganizationRepository(context(userId, MembershipRole.OWNER))
            val service = OrganizationService(
                repository,
                testAuthorization(userId, organizationId, PlatformPermission.ORGANIZATION_READ),
                fixedClock(),
            )

            assertEquals(organizationId.toString(), service.current(userId).id)
            assertFailsWith<OrganizationNotFoundException> {
                service.current(UUID.randomUUID())
            }
        }

    @Test
    fun `organization update requires its permission`() =
        runTest {
            val repository = MemoryOrganizationRepository(context(userId, MembershipRole.OWNER))
            val service = OrganizationService(
                repository,
                testAuthorization(userId, organizationId, PlatformPermission.ORGANIZATION_UPDATE),
                fixedClock(),
            )

            val updated = service.update(userId, UpdateOrganizationRequest(name = "Updated", slug = "NEW-SLUG"))
            assertEquals("Updated", updated.name)
            assertEquals("new-slug", updated.slug)

            val deniedService = OrganizationService(
                repository,
                testAuthorization(userId, organizationId),
                fixedClock(),
            )
            assertFailsWith<OrganizationAccessDeniedException> {
                deniedService.update(userId, UpdateOrganizationRequest(name = "Denied"))
            }
        }

    @Test
    fun `validates organization names slugs and empty updates`() =
        runTest {
            val service = OrganizationService(
                MemoryOrganizationRepository(context(userId, MembershipRole.OWNER)),
                testAuthorization(userId, organizationId, PlatformPermission.ORGANIZATION_UPDATE),
                fixedClock(),
            )

            assertFailsWith<OrganizationValidationException> {
                service.create(UUID.randomUUID(), CreateOrganizationRequest("A", "valid"))
            }
            assertFailsWith<OrganizationValidationException> {
                service.create(UUID.randomUUID(), CreateOrganizationRequest("Valid", "not valid"))
            }
            assertFailsWith<OrganizationValidationException> {
                service.update(userId, UpdateOrganizationRequest())
            }
        }

    @Test
    fun `owner suspends and reactivates the organization`() = runTest {
        val repository = MemoryOrganizationRepository(context(userId, MembershipRole.OWNER))
        val service = OrganizationService(
            repository,
            testAuthorization(userId, organizationId, PlatformPermission.ORGANIZATION_UPDATE),
            fixedClock(),
        )

        assertEquals("suspended", service.suspend(userId).status)
        assertEquals(null, repository.findCurrentByUser(userId))
        assertEquals("active", service.reactivate(userId).status)
    }

    @Test
    fun `non owner cannot change organization lifecycle`() = runTest {
        val repository = MemoryOrganizationRepository(context(userId, MembershipRole.MEMBER))
        val service = OrganizationService(
            repository,
            testAuthorization(userId, organizationId, PlatformPermission.ORGANIZATION_UPDATE),
            fixedClock(),
        )

        assertFailsWith<OrganizationAccessDeniedException> { service.suspend(userId) }
        assertFailsWith<OrganizationAccessDeniedException> { service.delete(userId) }
    }

    @Test
    fun `deleting organization removes retained membership`() = runTest {
        val repository = MemoryOrganizationRepository(context(userId, MembershipRole.OWNER))
        val service = OrganizationService(
            repository,
            testAuthorization(userId, organizationId, PlatformPermission.ORGANIZATION_UPDATE),
            fixedClock(),
        )

        service.delete(userId)

        assertEquals(OrganizationStatus.DELETED, repository.context?.organization?.status)
        assertEquals(MembershipStatus.REMOVED, repository.context?.membership?.status)
        assertEquals(null, repository.findRetainedByUser(userId))
    }

    private fun fixedClock() = Clock.fixed(now, ZoneOffset.UTC)

    private fun context(userId: UUID, role: MembershipRole): OrganizationContext =
        OrganizationContext(
            Organization(organizationId, "Acme", "acme", userId, now, now),
            OrganizationMembership(
                membershipId,
                organizationId,
                userId,
                role,
                MembershipStatus.ACTIVE,
                now,
                null,
            ),
        )
}

private class MemoryOrganizationRepository(
    var context: OrganizationContext? = null,
) : OrganizationRepository {
    override suspend fun createWithOwner(context: OrganizationContext): OrganizationContext {
        this.context = context
        return context
    }

    override suspend fun findCurrentByUser(userId: UUID): OrganizationContext? =
        context?.takeIf {
            it.membership.userId == userId && it.membership.status == MembershipStatus.ACTIVE &&
                it.organization.status == OrganizationStatus.ACTIVE
        }

    override suspend fun findRetainedByUser(userId: UUID): OrganizationContext? =
        context?.takeIf {
            it.membership.userId == userId && it.membership.status != MembershipStatus.REMOVED &&
                it.organization.status != OrganizationStatus.DELETED
        }

    override suspend fun update(organization: Organization): Organization {
        context = context?.copy(organization = organization)
        return organization
    }

    override suspend fun suspend(organizationId: UUID, updatedAt: Instant): Boolean {
        val current = context?.takeIf { it.organization.id == organizationId && it.organization.status == OrganizationStatus.ACTIVE }
            ?: return false
        context = current.copy(organization = current.organization.copy(status = OrganizationStatus.SUSPENDED, updatedAt = updatedAt))
        return true
    }

    override suspend fun reactivateOwnedBy(userId: UUID, updatedAt: Instant): OrganizationContext? {
        val current = context?.takeIf {
            it.membership.userId == userId && it.membership.role == MembershipRole.OWNER &&
                it.organization.status == OrganizationStatus.SUSPENDED
        } ?: return null
        return current.copy(organization = current.organization.copy(status = OrganizationStatus.ACTIVE, updatedAt = updatedAt))
            .also { context = it }
    }

    override suspend fun delete(organizationId: UUID, deletedAt: Instant): Boolean {
        val current = context?.takeIf { it.organization.id == organizationId && it.organization.status == OrganizationStatus.ACTIVE }
            ?: return false
        context = current.copy(
            organization = current.organization.copy(status = OrganizationStatus.DELETED, deletedAt = deletedAt),
            membership = current.membership.copy(status = MembershipStatus.REMOVED, removedAt = deletedAt),
        )
        return true
    }
}
