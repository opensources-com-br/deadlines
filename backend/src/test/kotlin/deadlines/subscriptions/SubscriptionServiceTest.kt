package deadlines.subscriptions

import deadlines.organizations.MembershipRole
import deadlines.organizations.MembershipStatus
import deadlines.organizations.Organization
import deadlines.organizations.OrganizationContext
import deadlines.organizations.OrganizationMembership
import deadlines.organizations.OrganizationRepository
import deadlines.plans.Plan
import deadlines.plans.PlanLimit
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SubscriptionServiceTest {
    private val now = Instant.parse("2026-09-06T20:00:00Z")

    @Test
    fun `returns only the active organization subscription`() = runTest {
        val userId = UUID.randomUUID()
        val organizationId = UUID.randomUUID()
        val subscription = subscription(organizationId)
        val service = SubscriptionService(OrganizationForSubscription(userId, organizationId, now), MemorySubscriptions(subscription))

        val response = service.current(userId)

        assertEquals(organizationId.toString(), response.organizationId)
        assertEquals("active", response.status)
        assertEquals("free", response.plan.key)
    }

    @Test
    fun `rejects a user without an active organization subscription`() = runTest {
        val service = SubscriptionService(OrganizationForSubscription(null, null, now), MemorySubscriptions(null))

        assertFailsWith<SubscriptionNotFoundException> { service.current(UUID.randomUUID()) }
    }

    private fun subscription(organizationId: UUID) =
        OrganizationSubscription(
            id = UUID.randomUUID(),
            organizationId = organizationId,
            plan = Plan(UUID.randomUUID(), "free", "Free", null, 0, "USD", listOf(PlanLimit("members", 3))),
            status = SubscriptionStatus.ACTIVE,
            startedAt = now,
            endedAt = null,
        )
}

private class OrganizationForSubscription(
    private val userId: UUID?,
    private val organizationId: UUID?,
    private val now: Instant,
) : OrganizationRepository {
    override suspend fun createWithOwner(context: OrganizationContext): OrganizationContext = context

    override suspend fun findCurrentByUser(userId: UUID): OrganizationContext? {
        if (userId != this.userId || organizationId == null) return null
        return OrganizationContext(
            organization = Organization(organizationId, "Acme", "acme", userId, now, now),
            membership = OrganizationMembership(UUID.randomUUID(), organizationId, userId, MembershipRole.OWNER, MembershipStatus.ACTIVE, now, null),
        )
    }

    override suspend fun update(organization: Organization): Organization = organization
}

private class MemorySubscriptions(private val subscription: OrganizationSubscription?) : SubscriptionRepository {
    override suspend fun findActiveByOrganization(organizationId: UUID): OrganizationSubscription? =
        subscription?.takeIf { it.organizationId == organizationId }
}
