package deadlines.subscriptions

import deadlines.organizations.authorization.AuthorizationOperations
import deadlines.organizations.authorization.PlatformPermission
import java.util.UUID

interface SubscriptionOperations {
    suspend fun current(userId: UUID): SubscriptionResponse
}

class SubscriptionService(
    private val authorization: AuthorizationOperations,
    private val subscriptions: SubscriptionRepository,
) : SubscriptionOperations {
    override suspend fun current(userId: UUID): SubscriptionResponse {
        val context = authorization.requirePermission(userId, PlatformPermission.BILLING_READ)
        return subscriptions.findActiveByOrganization(context.organizationId)
            ?.toResponse()
            ?: throw SubscriptionNotFoundException()
    }
}
