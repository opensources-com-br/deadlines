package deadlines.identity.email

import deadlines.config.EmailConfig
import deadlines.identity.auth.PasswordHasher
import deadlines.identity.auth.Session
import deadlines.identity.auth.SessionRepository
import deadlines.identity.users.User
import deadlines.identity.users.UserCredentials
import deadlines.identity.users.UserCredentialsRepository
import deadlines.identity.users.UserProfile
import deadlines.identity.users.UserStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordResetServiceTest {
    @Test
    fun `resets password and revokes every session`() = runTest {
        val now = Instant.parse("2026-09-06T12:00:00Z")
        val user = User(UUID.randomUUID(), "user@example.com", UserStatus.ACTIVE, UserProfile("User", "Test", null, null), now, now)
        val credentials = ResetCredentials(user)
        val sessions = ResetSessions()
        val service = PasswordResetService(credentials, ResetTokens(user.id), RecordingEmailService(), ResetHasher(), sessions, EmailConfig("x", "https://app", 3600, 3600), ResetTokenGenerator(), Clock.fixed(now, ZoneOffset.UTC))

        service.request(user.email)
        service.reset("fixed-token", "new-password-123")

        assertEquals("hash:new-password-123", credentials.hash)
        assertEquals(user.id, sessions.revokedUserId)
    }
}

private class ResetCredentials(private val user: User) : UserCredentialsRepository {
    var hash = "hash:old-password"
    override suspend fun create(user: User, passwordHash: String) = user
    override suspend fun findByEmail(email: String) = if (email == user.email) UserCredentials(user, hash) else null
    override suspend fun updatePassword(userId: UUID, passwordHash: String, updatedAt: Instant): Boolean { hash = passwordHash; return userId == user.id }
}
private class ResetTokens(private val userId: UUID) : EmailTokenRepository {
    override suspend fun createVerification(token: EmailToken) = Unit
    override suspend fun consumeVerification(tokenHash: String, now: Instant) = null
    override suspend fun createPasswordReset(token: EmailToken) = Unit
    override suspend fun consumePasswordReset(tokenHash: String, now: Instant) = userId
}
private class ResetTokenGenerator : EmailTokenGenerator { override fun generate() = "fixed-token"; override fun hash(token: String) = "h".repeat(64) }
private class ResetHasher : PasswordHasher { override suspend fun hash(password: String) = "hash:$password"; override suspend fun verify(password: String, hash: String) = false }
private class ResetSessions : SessionRepository {
    var revokedUserId: UUID? = null
    override suspend fun create(session: Session) = Unit
    override suspend fun findActive(refreshTokenHash: String, now: Instant) = null
    override suspend fun rotate(currentHash: String, replacement: Session, now: Instant) = false
    override suspend fun revoke(refreshTokenHash: String, now: Instant) = false
    override suspend fun revokeAll(userId: UUID, now: Instant): Int { revokedUserId = userId; return 1 }
    override suspend fun listActive(userId: UUID, now: Instant) = emptyList<Session>()
    override suspend fun revoke(userId: UUID, sessionId: UUID, now: Instant) = false
}
