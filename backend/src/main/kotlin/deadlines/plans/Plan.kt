package deadlines.plans

import java.util.UUID

data class Plan(
    val id: UUID,
    val key: String,
    val name: String,
    val description: String?,
    val monthlyPriceCents: Int,
    val currency: String,
    val limits: List<PlanLimit>,
)

data class PlanLimit(val resource: String, val value: Int)
