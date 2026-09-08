package opensources.modules.billing

import opensources.organizations.authorization.AuthorizationOperations
import opensources.modules.billing.plans.ExposedPlanRepository
import opensources.modules.billing.plans.PlanOperations
import opensources.modules.billing.plans.PlanService
import opensources.shared.database.DatabaseQuery
import opensources.modules.billing.subscriptions.ExposedSubscriptionRepository
import opensources.modules.billing.subscriptions.SubscriptionOperations
import opensources.modules.billing.subscriptions.SubscriptionService
import opensources.modules.billing.plans.planRoutes
import opensources.modules.billing.subscriptions.subscriptionRoutes
import io.ktor.server.routing.Route

data class BillingModule(
    val plans: PlanOperations,
    val subscriptions: SubscriptionOperations,
)

fun billingModule(query: DatabaseQuery, authorization: AuthorizationOperations): BillingModule =
    BillingModule(
        plans = PlanService(ExposedPlanRepository(query)),
        subscriptions = SubscriptionService(authorization, ExposedSubscriptionRepository(query)),
    )

fun Route.billingRoutes(module: BillingModule) {
    planRoutes(module.plans)
    subscriptionRoutes(module.subscriptions)
}
