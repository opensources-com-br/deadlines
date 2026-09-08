package opensources.identity.users

import opensources.identity.auth.InvalidCurrentPasswordException
import opensources.identity.auth.PasswordHasher
import java.util.UUID

class AccountPasswordVerifier(
    private val credentialsRepository: UserCredentialsRepository,
    private val passwordHasher: PasswordHasher,
) {
    suspend fun verify(userId: UUID, password: String) {
        val credentials = credentialsRepository.findByUserId(userId)
            ?: throw InvalidCurrentPasswordException()
        if (!passwordHasher.verify(password, credentials.passwordHash)) {
            throw InvalidCurrentPasswordException()
        }
    }
}
