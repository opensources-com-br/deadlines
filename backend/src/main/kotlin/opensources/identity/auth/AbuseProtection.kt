package opensources.identity.auth

import opensources.config.AbuseProtectionConfig
import opensources.shared.errors.ApiException
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

class RateLimitExceededException : ApiException(
    status = 429,
    code = "RATE_LIMITED",
    message = "Too many requests. Please try again later.",
)

interface RateLimitStore {
    suspend fun consume(key: String, limit: Int, windowSeconds: Long, now: Instant): Boolean
}

class LocalRateLimitStore : RateLimitStore {
    private val timestamps = ConcurrentHashMap<String, ArrayDeque<Instant>>()

    override suspend fun consume(key: String, limit: Int, windowSeconds: Long, now: Instant): Boolean = synchronized(timestamps) {
        val entries = timestamps.computeIfAbsent(key) { ArrayDeque() }
        val threshold = now.minusSeconds(windowSeconds)
        while (entries.firstOrNull()?.isBefore(threshold) == true) entries.removeFirst()
        if (entries.size >= limit) return false
        entries.addLast(now)
        true
    }
}

data class LoginAttempt(
    val emailHash: String,
    val ipHash: String,
    val attemptedAt: Instant,
    val successful: Boolean,
)

interface LoginAttemptStore {
    suspend fun record(attempt: LoginAttempt)
    suspend fun recentConsecutiveFailures(key: String, since: Instant): List<LoginAttempt>
}

class LocalLoginAttemptStore : LoginAttemptStore {
    private val attempts = ConcurrentHashMap<String, ArrayDeque<LoginAttempt>>()

    override suspend fun record(attempt: LoginAttempt) = synchronized(attempts) {
        attempts.computeIfAbsent("${attempt.emailHash}:${attempt.ipHash}") { ArrayDeque() }.addLast(attempt)
    }

    override suspend fun recentConsecutiveFailures(key: String, since: Instant): List<LoginAttempt> = synchronized(attempts) {
        val entries = attempts[key] ?: return emptyList()
        while (entries.firstOrNull()?.attemptedAt?.isBefore(since) == true) entries.removeFirst()
        entries.toList().asReversed().takeWhile { !it.successful }
    }
}

class AuthenticationAbuseProtection(
    private val config: AbuseProtectionConfig,
    private val rateLimits: RateLimitStore = LocalRateLimitStore(),
    private val loginAttempts: LoginAttemptStore = LocalLoginAttemptStore(),
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun checkRateLimit(route: String, ipAddress: String?, email: String? = null) {
        val now = clock.instant()
        val ipKey = "$route:ip:${hash(ipAddress.orEmpty())}"
        val emailKey = "$route:email:${hash(normalizeEmail(email).orEmpty())}"
        if (!rateLimits.consume(ipKey, config.rateLimitMaxRequests, config.rateLimitWindowSeconds, now) ||
            (email != null && !rateLimits.consume(emailKey, config.rateLimitMaxRequests, config.rateLimitWindowSeconds, now))
        ) {
            throw RateLimitExceededException()
        }
    }

    suspend fun checkLoginLockout(email: String, ipAddress: String?) {
        val now = clock.instant()
        val key = loginKey(email, ipAddress)
        val failures = loginAttempts.recentConsecutiveFailures(key, now.minusSeconds(config.loginLockoutMaxSeconds))
        if (failures.size < config.loginFailureThreshold) return
        val lockoutSeconds = (config.loginLockoutBaseSeconds * (1L shl (failures.size - config.loginFailureThreshold).coerceAtMost(30)))
            .coerceAtMost(config.loginLockoutMaxSeconds)
        if (failures.first().attemptedAt.plusSeconds(lockoutSeconds).isAfter(now)) throw RateLimitExceededException()
    }

    suspend fun recordLogin(email: String, ipAddress: String?, successful: Boolean) {
        val now = clock.instant()
        loginAttempts.record(LoginAttempt(hash(normalizeEmail(email)), hash(ipAddress.orEmpty()), now, successful))
    }

    private fun loginKey(email: String, ipAddress: String?) = "${hash(normalizeEmail(email))}:${hash(ipAddress.orEmpty())}"

    private fun normalizeEmail(email: String?) = email?.trim()?.lowercase().orEmpty()

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
