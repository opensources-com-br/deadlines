package deadlines.identity.auth

import deadlines.config.AuthConfig
import deadlines.identity.users.User
import deadlines.identity.users.UserAlreadyExistsException
import deadlines.identity.users.UserCredentials
import deadlines.identity.users.UserCredentialsRepository
import deadlines.identity.users.UserRepository
import deadlines.identity.users.UserProfile
import deadlines.identity.users.UserStatus
import deadlines.identity.email.EmailVerificationOperations
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthServiceTest {
    private val now = Instant.now()
    private val users = MemoryUsers()
    private val credentials = MemoryCredentials(users)
    private val sessions = MemorySessions()
    private val tokenService =
        TokenService(
            AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600),
            Clock.fixed(now, ZoneOffset.UTC),
        )
    private val service =
        AuthService(
            credentials,
            users,
            sessions,
            FakePasswordHasher(),
            tokenService,
            FakeEmailVerificationOperations(),
            Clock.fixed(now, ZoneOffset.UTC),
        )
    private val context = SessionContext("test-agent", "127.0.0.1")

    @Test
    fun `register creates a pending account and sends verification`() =
        runTest {
            val response = service.register(RegisterRequest(" USER@Example.com ", "password-123", " User ", " Name "), context)

            assertEquals("user@example.com", response.user.email)
            assertEquals("pending", response.user.status)
            assertEquals("hash:password-123", credentials.values["user@example.com"]?.passwordHash)
            assertEquals(0, sessions.values.size)
        }

    @Test
    fun `register rejects an existing email`() =
        runTest {
            val request = RegisterRequest("user@example.com", "password-123", "User", "Name")
            service.register(request, context)

            assertFailsWith<UserAlreadyExistsException> { service.register(request, context) }
        }

    @Test
    fun `login rejects invalid credentials`() =
        runTest {
            service.register(RegisterRequest("user@example.com", "password-123", "User", "Name"), context)

            assertFailsWith<InvalidCredentialsException> {
                service.login(LoginRequest("user@example.com", "wrong-password"), context)
            }
        }

    @Test
    fun `login rejects a pending account even with the correct password`() =
        runTest {
            service.register(RegisterRequest("user@example.com", "password-123", "User", "Name"), context)

            assertFailsWith<InvalidCredentialsException> {
                service.login(LoginRequest("user@example.com", "password-123"), context)
            }
        }

    @Test
    fun `refresh rotates the token and prevents reuse`() =
        runTest {
            createActiveUser()
            val authenticated = service.login(LoginRequest("user@example.com", "password-123"), context)
            val refreshed = service.refresh(authenticated.refreshToken, context)

            assertNotEquals(authenticated.refreshToken, refreshed.refreshToken)
            assertFailsWith<InvalidRefreshTokenException> { service.refresh(authenticated.refreshToken, context) }
        }

    @Test
    fun `logout revokes the refresh token`() =
        runTest {
            createActiveUser()
            val authenticated = service.login(LoginRequest("user@example.com", "password-123"), context)
            service.logout(authenticated.refreshToken)

            assertFailsWith<InvalidRefreshTokenException> { service.refresh(authenticated.refreshToken, context) }
        }

    @Test
    fun `change password verifies the current password and revokes sessions`() =
        runTest {
            val user = createActiveUser()
            val authenticated = service.login(LoginRequest("user@example.com", "password-123"), context)

            service.changePassword(user.id, ChangePasswordRequest("password-123", "new-password-123"))

            assertEquals("hash:new-password-123", credentials.values["user@example.com"]?.passwordHash)
            assertFailsWith<InvalidRefreshTokenException> { service.refresh(authenticated.refreshToken, context) }
        }

    @Test
    fun `change password rejects an invalid current password`() =
        runTest {
            val user = createActiveUser()

            assertFailsWith<InvalidCurrentPasswordException> {
                service.changePassword(user.id, ChangePasswordRequest("wrong-password", "new-password-123"))
            }
        }

    private suspend fun createActiveUser(): User {
        val user = User(UUID.randomUUID(), "user@example.com", UserStatus.ACTIVE, UserProfile("User", "Name", null, null), now, now, now)
        credentials.create(user, "hash:password-123")
        return user
    }
}

private class FakePasswordHasher : PasswordHasher {
    override suspend fun hash(password: String) = "hash:$password"

    override suspend fun verify(password: String, hash: String) = hash == "hash:$password"
}

private class FakeEmailVerificationOperations : EmailVerificationOperations {
    override suspend fun resend(userId: UUID) = true
    override suspend fun resendForEmail(email: String) = true
    override suspend fun verify(rawToken: String) = Unit
}

private class MemoryUsers : UserRepository {
    val values = linkedMapOf<String, User>()

    override suspend fun create(user: User): User = user.also { values[it.email] = it }

    override suspend fun findById(id: UUID) = values.values.firstOrNull { it.id == id }

    override suspend fun findByEmail(email: String) = values[email.lowercase()]

    override suspend fun list(offset: Long, limit: Int) = values.values.drop(offset.toInt()).take(limit)

    override suspend fun count() = values.size.toLong()

    override suspend fun update(user: User): User = user.also { values[it.email] = it }

    override suspend fun markEmailVerified(id: UUID, verifiedAt: Instant): User? {
        val current = findById(id) ?: return null
        val updated = current.copy(status = UserStatus.ACTIVE, emailVerifiedAt = verifiedAt, updatedAt = verifiedAt)
        values[updated.email] = updated
        return updated
    }
}

private class MemoryCredentials(
    private val users: MemoryUsers,
) : UserCredentialsRepository {
    val values = linkedMapOf<String, UserCredentials>()

    override suspend fun create(user: User, passwordHash: String): User {
        users.create(user)
        values[user.email] = UserCredentials(user, passwordHash)
        return user
    }

    override suspend fun findByEmail(email: String): UserCredentials? = values[email.lowercase()]

    override suspend fun updatePassword(userId: UUID, passwordHash: String, updatedAt: Instant): Boolean {
        val entry = values.entries.firstOrNull { it.value.user.id == userId } ?: return false
        values[entry.key] = entry.value.copy(passwordHash = passwordHash)
        return true
    }
}

private class MemorySessions : SessionRepository {
    val values = mutableListOf<Session>()
    private val revoked = mutableSetOf<String>()

    override suspend fun create(session: Session) {
        values += session
    }

    override suspend fun findActive(refreshTokenHash: String, now: Instant) =
        values.firstOrNull { it.refreshTokenHash == refreshTokenHash && it.refreshTokenHash !in revoked && it.expiresAt > now }

    override suspend fun rotate(currentHash: String, replacement: Session, now: Instant): Boolean {
        if (findActive(currentHash, now) == null) return false
        revoked += currentHash
        values += replacement
        return true
    }

    override suspend fun revoke(refreshTokenHash: String, now: Instant): Boolean = revoked.add(refreshTokenHash)

    override suspend fun revokeAll(userId: UUID, now: Instant): Int =
        values.filter { it.userId == userId && revoked.add(it.refreshTokenHash) }.size
}
