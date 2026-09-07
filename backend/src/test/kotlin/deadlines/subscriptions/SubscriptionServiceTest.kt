package deadlines.subscriptions

import deadlines.organizations.authorization.PlatformPermission
import deadlines.organizations.authorization.testAuthorization
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
        val service = SubscriptionService(
            testAuthorization(userId, organizationId, PlatformPermission.BILLING_READ),
            MemorySubscriptions(subscription),
        )

        val response = service.current(userId)

        assertEquals(organizationId.toString(), response.organizationId)
        assertEquals("active", response.status)
        assertEquals("free", response.plan.key)
    }

    @Test
    fun `rejects a user without an active organization subscription`() = runTest {
        val userId = UUID.randomUUID()
        val service = SubscriptionService(
            testAuthorization(userId, UUID.randomUUID(), PlatformPermission.BILLING_READ),
            MemorySubscriptions(null),
        )

        assertFailsWith<SubscriptionNotFoundException> { service.current(userId) }
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

private class MemorySubscriptions(private val subscription: OrganizationSubscription?) : SubscriptionRepository {
    override suspend fun findActiveByOrganization(organizationId: UUID): OrganizationSubscription? =
        subscription?.takeIf { it.organizationId == organizationId }
}
