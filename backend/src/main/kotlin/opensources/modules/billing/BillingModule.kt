package opensources.modules.billing

import opensources.organizations.authorization.AuthorizationOperations
import opensources.plans.ExposedPlanRepository
import opensources.plans.PlanOperations
import opensources.plans.PlanService
import opensources.shared.database.DatabaseQuery
import opensources.subscriptions.ExposedSubscriptionRepository
import opensources.subscriptions.SubscriptionOperations
import opensources.subscriptions.SubscriptionService

data class BillingModule(
    val plans: PlanOperations,
    val subscriptions: SubscriptionOperations,
)

fun billingModule(query: DatabaseQuery, authorization: AuthorizationOperations): BillingModule =
    BillingModule(
        plans = PlanService(ExposedPlanRepository(query)),
        subscriptions = SubscriptionService(authorization, ExposedSubscriptionRepository(query)),
    )
