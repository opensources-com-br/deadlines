package deadlines.plans

import deadlines.shared.database.DatabaseQuery
import java.util.UUID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.selectAll

interface PlanRepository { suspend fun listActive(): List<Plan> }

class ExposedPlanRepository(private val query: DatabaseQuery) : PlanRepository {
    override suspend fun listActive(): List<Plan> = query {
        val limits = PlanLimits.selectAll().map { it[PlanLimits.planId] to PlanLimit(it[PlanLimits.resource], it[PlanLimits.value]) }.groupBy({ it.first }, { it.second })
        Plans.selectAll().where { Plans.isActive eq true }.orderBy(Plans.displayOrder).map {
            Plan(it[Plans.id], it[Plans.key], it[Plans.name], it[Plans.description], it[Plans.monthlyPriceCents], it[Plans.currency], limits[it[Plans.id]].orEmpty())
        }
    }
}

private object Plans : Table("plans") {
    val id = javaUUID("id"); val key = varchar("key", 80); val name = varchar("name", 120); val description = text("description").nullable()
    val monthlyPriceCents = integer("monthly_price_cents"); val currency = char("currency", 3); val isActive = bool("is_active"); val displayOrder = integer("display_order")
}
private object PlanLimits : Table("plan_limits") { val planId = javaUUID("plan_id"); val resource = varchar("resource", 80); val value = integer("limit_value") }
