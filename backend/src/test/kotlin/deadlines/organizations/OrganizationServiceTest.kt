package deadlines.organizations

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
            val service = OrganizationService(repository, fixedClock(), ids::removeFirst)

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
            val service = OrganizationService(repository, fixedClock())

            assertFailsWith<ActiveMembershipAlreadyExistsException> {
                service.create(userId, CreateOrganizationRequest("Another", "another"))
            }
        }

    @Test
    fun `returns current organization and reports missing onboarding`() =
        runTest {
            val repository = MemoryOrganizationRepository(context(userId, MembershipRole.OWNER))
            val service = OrganizationService(repository, fixedClock())

            assertEquals(organizationId.toString(), service.current(userId).id)
            assertFailsWith<OrganizationNotFoundException> {
                service.current(UUID.randomUUID())
            }
        }

    @Test
    fun `only an owner can update a valid organization`() =
        runTest {
            val repository = MemoryOrganizationRepository(context(userId, MembershipRole.OWNER))
            val service = OrganizationService(repository, fixedClock())

            val updated = service.update(userId, UpdateOrganizationRequest(name = "Updated", slug = "NEW-SLUG"))
            assertEquals("Updated", updated.name)
            assertEquals("new-slug", updated.slug)

            repository.context = context(userId, MembershipRole.MEMBER)
            assertFailsWith<OrganizationAccessDeniedException> {
                service.update(userId, UpdateOrganizationRequest(name = "Denied"))
            }
        }

    @Test
    fun `validates organization names slugs and empty updates`() =
        runTest {
            val service = OrganizationService(MemoryOrganizationRepository(context(userId, MembershipRole.OWNER)), fixedClock())

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
        context?.takeIf { it.membership.userId == userId && it.membership.status == MembershipStatus.ACTIVE }

    override suspend fun update(organization: Organization): Organization {
        context = context?.copy(organization = organization)
        return organization
    }
}
