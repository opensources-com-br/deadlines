package deadlines.subscriptions

import deadlines.plans.PlanResponse
import deadlines.plans.toResponse
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionResponse(
    val id: String,
    val organizationId: String,
    val status: String,
    val startedAt: String,
    val endedAt: String?,
    val plan: PlanResponse,
)

fun OrganizationSubscription.toResponse() =
    SubscriptionResponse(
        id = id.toString(),
        organizationId = organizationId.toString(),
        status = status.name.lowercase(),
        startedAt = startedAt.toString(),
        endedAt = endedAt?.toString(),
        plan = plan.toResponse(),
    )
