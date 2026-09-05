package deadlines.identity.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PasswordHasher {
    suspend fun hash(password: String): String

    suspend fun verify(password: String, hash: String): Boolean
}

class BcryptPasswordHasher(
    private val cost: Int = 12,
) : PasswordHasher {
    init {
        require(cost in 10..16) { "BCrypt cost must be between 10 and 16" }
    }

    override suspend fun hash(password: String): String =
        withContext(Dispatchers.Default) {
            BCrypt.withDefaults().hashToString(cost, password.toCharArray())
        }

    override suspend fun verify(password: String, hash: String): Boolean =
        withContext(Dispatchers.Default) {
            BCrypt.verifyer().verify(password.toCharArray(), hash).verified
        }
}
