package deadlines.subscriptions

import deadlines.plans.Plan
import java.time.Instant
import java.util.UUID

enum class SubscriptionStatus {
    ACTIVE,
    CANCELED,
}

data class OrganizationSubscription(
    val id: UUID,
    val organizationId: UUID,
    val plan: Plan,
    val status: SubscriptionStatus,
    val startedAt: Instant,
    val endedAt: Instant?,
)
