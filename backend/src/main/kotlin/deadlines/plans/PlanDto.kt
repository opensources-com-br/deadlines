package deadlines.plans

import kotlinx.serialization.Serializable

@Serializable
data class PlanResponse(
    val id: String,
    val key: String,
    val name: String,
    val description: String?,
    val monthlyPriceCents: Int,
    val currency: String,
    val limits: List<PlanLimitResponse>,
)

@Serializable
data class PlanLimitResponse(val resource: String, val value: Int)

@Serializable
data class PlanListResponse(val data: List<PlanResponse>)

fun Plan.toResponse() = PlanResponse(id.toString(), key, name, description, monthlyPriceCents, currency, limits.map { PlanLimitResponse(it.resource, it.value) })
