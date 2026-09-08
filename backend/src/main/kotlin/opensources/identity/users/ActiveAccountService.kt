package opensources.identity.users

import java.util.UUID

interface ActiveAccountOperations {
    suspend fun isActive(userId: UUID): Boolean

    suspend fun requireActive(userId: UUID): User
}

class ActiveAccountService(
    private val users: UserRepository,
) : ActiveAccountOperations {
    override suspend fun isActive(userId: UUID): Boolean =
        users.findById(userId)?.status == UserStatus.ACTIVE

    override suspend fun requireActive(userId: UUID): User =
        users.findById(userId)
            ?.takeIf { it.status == UserStatus.ACTIVE }
            ?: throw AccountNotActiveException()
}
