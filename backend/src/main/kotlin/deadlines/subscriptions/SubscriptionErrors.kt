package deadlines.subscriptions

import deadlines.shared.errors.ApiException

class SubscriptionNotFoundException : ApiException(
    status = 404,
    code = "SUBSCRIPTION_NOT_FOUND",
    message = "No active subscription was found for this organization",
)
