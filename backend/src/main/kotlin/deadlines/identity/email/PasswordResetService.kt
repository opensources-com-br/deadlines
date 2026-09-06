package deadlines.identity.email

import deadlines.config.EmailConfig
import deadlines.identity.auth.PasswordHasher
import deadlines.identity.auth.SessionRepository
import deadlines.identity.users.UserCredentialsRepository
import java.time.Clock
import java.util.UUID

class InvalidPasswordResetTokenException : deadlines.shared.errors.ApiException(
    status = 400,
    code = "INVALID_PASSWORD_RESET_TOKEN",
    message = "Password reset token is invalid or expired",
)

class PasswordResetValidationException : deadlines.shared.errors.ApiException(
    status = 422,
    code = "VALIDATION_ERROR",
    message = "Invalid password reset data",
    details = mapOf("password" to "must contain between 12 and 72 characters"),
)

class PasswordResetService(
    private val credentials: UserCredentialsRepository,
    private val tokens: EmailTokenRepository,
    private val email: EmailService,
    private val passwordHasher: PasswordHasher,
    private val sessions: SessionRepository,
    private val config: EmailConfig,
    private val tokenGenerator: EmailTokenGenerator = SecureEmailTokenGenerator(),
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun request(emailAddress: String) {
        val credentials = credentials.findByEmail(emailAddress.trim().lowercase()) ?: return
        val now = clock.instant()
        val rawToken = tokenGenerator.generate()
        tokens.createPasswordReset(
            EmailToken(UUID.randomUUID(), credentials.user.id, tokenGenerator.hash(rawToken), now.plusSeconds(config.passwordResetExpirationSeconds), now),
        )
        email.send(
            EmailMessage(
                credentials.user.email,
                "Reset your password",
                "Reset your password: ${config.appBaseUrl}/reset-password?token=$rawToken",
            ),
        )
    }

    suspend fun reset(rawToken: String, password: String) {
        if (password.length !in 12..72) throw PasswordResetValidationException()
        val now = clock.instant()
        val userId = tokens.consumePasswordReset(tokenGenerator.hash(rawToken), now) ?: throw InvalidPasswordResetTokenException()
        if (!credentials.updatePassword(userId, passwordHasher.hash(password), now)) throw InvalidPasswordResetTokenException()
        sessions.revokeAll(userId, now)
    }
}
