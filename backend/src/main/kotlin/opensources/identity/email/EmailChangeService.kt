package opensources.identity.email

import opensources.config.EmailConfig
import opensources.identity.users.AccountNotActiveException
import opensources.identity.users.AccountPasswordVerifier
import opensources.identity.users.UserAlreadyExistsException
import opensources.identity.users.UserRepository
import opensources.identity.users.UserStatus
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class EmailChangeToken(
    val id: UUID,
    val userId: UUID,
    val newEmail: String,
    val tokenHash: String,
    val expiresAt: Instant,
    val createdAt: Instant,
)

interface EmailChangeRepository {
    suspend fun create(token: EmailChangeToken)
    suspend fun confirm(tokenHash: String, now: Instant): UUID?
}

interface EmailChangeOperations {
    suspend fun request(userId: UUID, newEmail: String, password: String)
    suspend fun confirm(token: String)
}

class EmailChangeService(
    private val users: UserRepository,
    private val passwords: AccountPasswordVerifier,
    private val changes: EmailChangeRepository,
    private val email: EmailService,
    private val config: EmailConfig,
    private val tokenGenerator: EmailTokenGenerator = SecureEmailTokenGenerator(),
    private val clock: Clock = Clock.systemUTC(),
) : EmailChangeOperations {
    override suspend fun request(userId: UUID, newEmail: String, password: String) {
        val user = users.findById(userId)?.takeIf { it.status == UserStatus.ACTIVE } ?: throw AccountNotActiveException()
        val normalizedEmail = newEmail.trim().lowercase()
        if (!EMAIL_PATTERN.matches(normalizedEmail) || normalizedEmail.length > 320) {
            throw EmailChangeValidationException()
        }
        if (normalizedEmail == user.email || users.findByEmail(normalizedEmail) != null) throw UserAlreadyExistsException()
        passwords.verify(userId, password)

        val now = clock.instant()
        val rawToken = tokenGenerator.generate()
        changes.create(
            EmailChangeToken(
                UUID.randomUUID(), userId, normalizedEmail, tokenGenerator.hash(rawToken),
                now.plusSeconds(config.verificationExpirationSeconds), now,
            ),
        )
        email.send(
            EmailMessage(
                to = normalizedEmail,
                subject = "Confirm your new email",
                text = "Confirm your new email: ${config.appBaseUrl}/confirm-email-change?token=$rawToken",
            ),
        )
    }

    override suspend fun confirm(token: String) {
        if (changes.confirm(tokenGenerator.hash(token), clock.instant()) == null) {
            throw InvalidEmailChangeTokenException()
        }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
