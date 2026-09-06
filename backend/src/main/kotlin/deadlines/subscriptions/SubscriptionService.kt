package deadlines.subscriptions

import deadlines.organizations.OrganizationRepository
import java.util.UUID

interface SubscriptionOperations {
    suspend fun current(userId: UUID): SubscriptionResponse
}

class SubscriptionService(
    private val organizations: OrganizationRepository,
    private val subscriptions: SubscriptionRepository,
) : SubscriptionOperations {
    override suspend fun current(userId: UUID): SubscriptionResponse {
        val organization = organizations.findCurrentByUser(userId)
            ?: throw SubscriptionNotFoundException()
        return subscriptions.findActiveByOrganization(organization.organization.id)
            ?.toResponse()
            ?: throw SubscriptionNotFoundException()
    }
}
