package deadlines.identity.email

import deadlines.config.EmailConfig
import deadlines.identity.users.UserNotFoundException
import deadlines.identity.users.UserRepository
import deadlines.identity.users.UserStatus
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.UUID

interface EmailTokenGenerator {
    fun generate(): String
    fun hash(token: String): String
}

class SecureEmailTokenGenerator : EmailTokenGenerator {
    private val random = SecureRandom()

    override fun generate(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))

    override fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
}

interface EmailVerificationOperations {
    suspend fun resend(userId: UUID): Boolean
    suspend fun resendForEmail(email: String): Boolean
    suspend fun verify(rawToken: String)
}

class EmailVerificationService(
    private val users: UserRepository,
    private val tokens: EmailTokenRepository,
    private val email: EmailService,
    private val config: EmailConfig,
    private val tokenGenerator: EmailTokenGenerator = SecureEmailTokenGenerator(),
    private val clock: Clock = Clock.systemUTC(),
) : EmailVerificationOperations {
    override suspend fun resend(userId: UUID): Boolean {
        val user = users.findById(userId) ?: throw UserNotFoundException()
        if (user.emailVerifiedAt != null || user.status != UserStatus.PENDING) return false

        val now = clock.instant()
        val rawToken = tokenGenerator.generate()
        tokens.createVerification(
            EmailToken(UUID.randomUUID(), user.id, tokenGenerator.hash(rawToken), now.plusSeconds(config.verificationExpirationSeconds), now),
        )
        email.send(
            EmailMessage(
                to = user.email,
                subject = "Confirm your email",
                text = "Confirm your email: ${config.appBaseUrl}/verify-email?token=$rawToken",
            ),
        )
        return true
    }

    override suspend fun resendForEmail(email: String): Boolean {
        val user = users.findByEmail(email.trim().lowercase()) ?: return false
        return resend(user.id)
    }

    override suspend fun verify(rawToken: String) {
        val now = clock.instant()
        val userId = tokens.consumeVerification(tokenGenerator.hash(rawToken), now) ?: throw InvalidEmailVerificationTokenException()
        users.markEmailVerified(userId, now) ?: throw UserNotFoundException()
    }
}
