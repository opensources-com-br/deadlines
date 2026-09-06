package deadlines.identity.email

import deadlines.config.EmailConfig
import deadlines.identity.users.InMemoryUserRepository
import deadlines.identity.users.User
import deadlines.identity.users.UserProfile
import deadlines.identity.users.UserStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailVerificationServiceTest {
    private val now = Instant.parse("2026-09-06T12:00:00Z")

    @Test
    fun `sends and consumes a single-use verification token`() =
        runTest {
            val users = InMemoryUserRepository()
            val user = User(UUID.randomUUID(), "user@example.com", UserStatus.ACTIVE, UserProfile("User", "Test", null, null), now, now)
            users.create(user)
            val tokens = MemoryEmailTokens()
            val messages = RecordingEmailService()
            val service = EmailVerificationService(users, tokens, messages, config(), FixedTokenGenerator(), Clock.fixed(now, ZoneOffset.UTC))

            assertTrue(service.resend(user.id))
            assertTrue(messages.sentMessages.single().text.contains("token=fixed-token"))
            service.verify("fixed-token")

            assertEquals(now, users.findById(user.id)?.emailVerifiedAt)
            assertFalse(service.resend(user.id))
        }

    private fun config() = EmailConfig("no-reply@example.com", "https://app.example.com", 3600, 3600)
}

private class FixedTokenGenerator : EmailTokenGenerator {
    override fun generate() = "fixed-token"
    override fun hash(token: String) = "h".repeat(64)
}

private class MemoryEmailTokens : EmailTokenRepository {
    private var verification: EmailToken? = null
    override suspend fun createVerification(token: EmailToken) { verification = token }
    override suspend fun consumeVerification(tokenHash: String, now: Instant): UUID? =
        verification?.takeIf { it.tokenHash == tokenHash && it.expiresAt > now }?.userId.also { verification = null }
    override suspend fun createPasswordReset(token: EmailToken) = Unit
    override suspend fun consumePasswordReset(tokenHash: String, now: Instant): UUID? = null
}
