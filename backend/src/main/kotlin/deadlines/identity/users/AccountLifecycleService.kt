package deadlines.identity.users

import deadlines.organizations.MembershipRole
import deadlines.organizations.OrganizationRepository
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
    private val users: UserRepository,
    private val organizations: OrganizationRepository,
    private val passwords: AccountPasswordVerifier,
    private val lifecycle: AccountLifecycleRepository,
    private val clock: Clock = Clock.systemUTC(),
) : AccountLifecycleOperations {
    override suspend fun deactivate(userId: UUID, password: String) {
        requireActiveAccount(userId)
        requireNotOwner(userId)
        passwords.verify(userId, password)
        if (!lifecycle.deactivate(userId, clock.instant())) throw UserNotFoundException()
    }

    override suspend fun delete(userId: UUID, password: String) {
        requireActiveAccount(userId)
        requireNotOwner(userId)
        passwords.verify(userId, password)
        if (!lifecycle.delete(userId, clock.instant())) throw UserNotFoundException()
    }

    private suspend fun requireActiveAccount(userId: UUID) {
        val user = users.findById(userId) ?: throw UserNotFoundException()
        if (user.status != UserStatus.ACTIVE) throw AccountNotActiveException()
    }

    private suspend fun requireNotOwner(userId: UUID) {
        val membership = organizations.findRetainedByUser(userId)?.membership ?: return
        if (membership.role == MembershipRole.OWNER) throw AccountOwnerConflictException()
    }
}
