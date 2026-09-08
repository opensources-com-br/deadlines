package deadlines.modules.billing

import deadlines.organizations.authorization.AuthorizationOperations
import deadlines.plans.ExposedPlanRepository
import deadlines.plans.PlanOperations
import deadlines.plans.PlanService
import deadlines.shared.database.DatabaseQuery
import deadlines.subscriptions.ExposedSubscriptionRepository
import deadlines.subscriptions.SubscriptionOperations
import deadlines.subscriptions.SubscriptionService

data class BillingModule(
    val plans: PlanOperations,
    val subscriptions: SubscriptionOperations,
)

fun billingModule(query: DatabaseQuery, authorization: AuthorizationOperations): BillingModule =
    BillingModule(
        plans = PlanService(ExposedPlanRepository(query)),
        subscriptions = SubscriptionService(authorization, ExposedSubscriptionRepository(query)),
    )
