package opensources.identity.users

import opensources.organizations.MembershipRole
import opensources.organizations.OrganizationRepository
import opensources.organizations.audits.withAuditActor
import java.time.Clock
import java.time.Instant
import java.util.UUID

interface AccountLifecycleRepository {
    suspend fun deactivate(userId: UUID, disabledAt: Instant): Boolean

    suspend fun delete(userId: UUID, deletedAt: Instant): Boolean
}

interface AccountLifecycleOperations {
    suspend fun deactivate(userId: UUID, password: String)

    suspend fun delete(userId: UUID, password: String)
}

class AccountLifecycleService(
    users: UserRepository,
    private val organizations: OrganizationRepository,
    private val passwords: AccountPasswordVerifier,
    private val lifecycle: AccountLifecycleRepository,
    private val clock: Clock = Clock.systemUTC(),
) : AccountLifecycleOperations {
    private val activeAccounts = ActiveAccountService(users)
    override suspend fun deactivate(userId: UUID, password: String) = withAuditActor(userId) {
        activeAccounts.requireActive(userId)
        requireNotOwner(userId)
        passwords.verify(userId, password)
        if (!lifecycle.deactivate(userId, clock.instant())) throw UserNotFoundException()
    }

    override suspend fun delete(userId: UUID, password: String) = withAuditActor(userId) {
        activeAccounts.requireActive(userId)
        requireNotOwner(userId)
        passwords.verify(userId, password)
        if (!lifecycle.delete(userId, clock.instant())) throw UserNotFoundException()
    }

    private suspend fun requireNotOwner(userId: UUID) {
        val membership = organizations.findRetainedByUser(userId)?.membership ?: return
        if (membership.role == MembershipRole.OWNER) throw AccountOwnerConflictException()
    }
}
