package opensources.identity.email

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import opensources.config.EmailConfig
import opensources.identity.auth.PasswordHasher
import opensources.identity.users.AccountPasswordVerifier
import opensources.identity.users.InMemoryUserRepository
import opensources.identity.users.User
import opensources.identity.users.UserCredentials
import opensources.identity.users.UserCredentialsRepository
import opensources.identity.users.UserProfile
import opensources.identity.users.UserStatus

class EmailChangeServiceTest {
    private val now = Instant.parse("2026-09-08T12:00:00Z")

    @Test
    fun `confirms an email change only with the latest valid token`() = runTest {
        val users = InMemoryUserRepository()
        val user = User(UUID.randomUUID(), "old@example.com", UserStatus.ACTIVE, UserProfile("User", "Name", null, null), now, now, now)
        users.create(user)
        val changes = MemoryChanges(users)
        val messages = RecordingEmailService()
        val service = EmailChangeService(
            users, AccountPasswordVerifier(Credentials(user), PlainHasher), changes, messages,
            EmailConfig("no-reply@example.com", "https://app.example.com", 3600, 3600), SequenceGenerator(), Clock.fixed(now, ZoneOffset.UTC),
        )

        service.request(user.id, "new@example.com", "password")
        assertTrue(messages.sentMessages.single().text.contains("token=token-1"))
        service.request(user.id, "newer@example.com", "password")
        assertFailsWith<InvalidEmailChangeTokenException> { service.confirm("token-1") }
        service.confirm("token-2")

        assertEquals("newer@example.com", users.findById(user.id)?.email)
    }
}

private class MemoryChanges(private val users: InMemoryUserRepository) : EmailChangeRepository {
    private var token: EmailChangeToken? = null
    override suspend fun create(token: EmailChangeToken) { this.token = token }
    override suspend fun confirm(tokenHash: String, now: Instant): UUID? {
        val current = token?.takeIf { it.tokenHash == tokenHash && it.expiresAt > now } ?: return null
        val user = users.findById(current.userId) ?: return null
        users.update(user.copy(email = current.newEmail, updatedAt = now))
        token = null
        return current.userId
    }
}

private class Credentials(private val user: User) : UserCredentialsRepository {
    override suspend fun create(user: User, passwordHash: String) = user
    override suspend fun findByEmail(email: String) = UserCredentials(user, "password")
    override suspend fun findByUserId(userId: UUID) = UserCredentials(user, "password")
    override suspend fun updatePassword(userId: UUID, passwordHash: String, updatedAt: Instant) = false
}

private object PlainHasher : PasswordHasher {
    override suspend fun hash(password: String) = password
    override suspend fun verify(password: String, hash: String) = password == hash
}

private class SequenceGenerator : EmailTokenGenerator {
    private var sequence = 0
    override fun generate() = "token-${++sequence}"
    override fun hash(token: String) = token
}
