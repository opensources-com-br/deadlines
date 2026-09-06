package deadlines.subscriptions

import deadlines.plans.Plan
import deadlines.plans.PlanLimit
import deadlines.shared.database.DatabaseQuery
import java.util.UUID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.selectAll

interface SubscriptionRepository {
    suspend fun findActiveByOrganization(organizationId: UUID): OrganizationSubscription?
}

class ExposedSubscriptionRepository(
    private val query: DatabaseQuery,
) : SubscriptionRepository {
    override suspend fun findActiveByOrganization(organizationId: UUID): OrganizationSubscription? =
        query {
            val row =
                (OrganizationSubscriptions innerJoin SubscriptionPlans).selectAll()
                    .where {
                        (OrganizationSubscriptions.organizationId eq organizationId) and
                            (OrganizationSubscriptions.status eq SubscriptionStatus.ACTIVE.name.lowercase())
                    }.singleOrNull() ?: return@query null
            val planId = row[SubscriptionPlans.id]
            val limits =
                SubscriptionPlanLimits.selectAll()
                    .where { SubscriptionPlanLimits.planId eq planId }
                    .map { PlanLimit(it[SubscriptionPlanLimits.resource], it[SubscriptionPlanLimits.value]) }
            OrganizationSubscription(
                id = row[OrganizationSubscriptions.id],
                organizationId = row[OrganizationSubscriptions.organizationId],
                plan = Plan(
                    id = planId,
                    key = row[SubscriptionPlans.key],
                    name = row[SubscriptionPlans.name],
                    description = row[SubscriptionPlans.description],
                    monthlyPriceCents = row[SubscriptionPlans.monthlyPriceCents],
                    currency = row[SubscriptionPlans.currency],
                    limits = limits,
                ),
                status = SubscriptionStatus.valueOf(row[OrganizationSubscriptions.status].uppercase()),
                startedAt = row[OrganizationSubscriptions.startedAt].toInstant(),
                endedAt = row[OrganizationSubscriptions.endedAt]?.toInstant(),
            )
        }
}

private object SubscriptionOrganizations : Table("organizations") {
    val id = javaUUID("id")
}

private object SubscriptionPlans : Table("plans") {
    val id = javaUUID("id")
    val key = varchar("key", 80)
    val name = varchar("name", 120)
    val description = text("description").nullable()
    val monthlyPriceCents = integer("monthly_price_cents")
    val currency = char("currency", 3)
}

private object OrganizationSubscriptions : Table("organization_subscriptions") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(SubscriptionOrganizations.id)
    val planId = javaUUID("plan_id").references(SubscriptionPlans.id)
    val status = varchar("status", 32)
    val startedAt = timestampWithTimeZone("started_at")
    val endedAt = timestampWithTimeZone("ended_at").nullable()
}

private object SubscriptionPlanLimits : Table("plan_limits") {
    val planId = javaUUID("plan_id").references(SubscriptionPlans.id)
    val resource = varchar("resource", 80)
    val value = integer("limit_value")
}
