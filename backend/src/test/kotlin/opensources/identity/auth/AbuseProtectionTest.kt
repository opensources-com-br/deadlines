package opensources.identity.auth

import opensources.config.AbuseProtectionConfig
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlinx.coroutines.test.runTest

class AbuseProtectionTest {
    @Test
    fun `limits requests by IP and normalized email`() = runTest {
        val protection = AuthenticationAbuseProtection(config(rateLimitMaxRequests = 2))

        protection.checkRateLimit("register", "127.0.0.1", "USER@example.com")
        protection.checkRateLimit("register", "127.0.0.1", "user@example.com")

        assertFailsWith<RateLimitExceededException> {
            protection.checkRateLimit("register", "127.0.0.1", "user@example.com")
        }
    }

    @Test
    fun `locks repeated failed logins progressively`() = runTest {
        val clock = MutableClock(Instant.parse("2026-09-08T12:00:00Z"))
        val protection = AuthenticationAbuseProtection(config(loginFailureThreshold = 2), clock = clock)

        protection.recordLogin("user@example.com", "127.0.0.1", successful = false)
        protection.recordLogin("user@example.com", "127.0.0.1", successful = false)
        assertFailsWith<RateLimitExceededException> { protection.checkLoginLockout("user@example.com", "127.0.0.1") }

        clock.advanceSeconds(60)
        protection.checkLoginLockout("user@example.com", "127.0.0.1")
        protection.recordLogin("user@example.com", "127.0.0.1", successful = false)
        assertFailsWith<RateLimitExceededException> { protection.checkLoginLockout("user@example.com", "127.0.0.1") }

        clock.advanceSeconds(120)
        protection.checkLoginLockout("user@example.com", "127.0.0.1")
    }

    @Test
    fun `stores only hashes in login attempts`() = runTest {
        val store = RecordingLoginAttemptStore()
        val protection = AuthenticationAbuseProtection(config(), loginAttempts = store)

        protection.recordLogin("User@example.com", "127.0.0.1", successful = false)

        assertNotEquals("user@example.com", store.attempt.emailHash)
        assertNotEquals("127.0.0.1", store.attempt.ipHash)
        assertEquals(false, store.attempt.successful)
    }

    private fun config(
        rateLimitMaxRequests: Int = 10,
        loginFailureThreshold: Int = 5,
    ) =
        AbuseProtectionConfig(
            rateLimitWindowSeconds = 60,
            rateLimitMaxRequests = rateLimitMaxRequests,
            loginFailureThreshold = loginFailureThreshold,
            loginLockoutBaseSeconds = 60,
            loginLockoutMaxSeconds = 240,
        )
}

private class MutableClock(
    private var current: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = current

    fun advanceSeconds(seconds: Long) {
        current = current.plusSeconds(seconds)
    }
}

private class RecordingLoginAttemptStore : LoginAttemptStore {
    lateinit var attempt: LoginAttempt

    override suspend fun record(attempt: LoginAttempt) {
        this.attempt = attempt
    }

    override suspend fun recentConsecutiveFailures(key: String, since: Instant): List<LoginAttempt> = emptyList()
}
