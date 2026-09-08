package opensources.identity.auth

import opensources.config.AbuseProtectionConfig
import opensources.shared.errors.ApiException
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

class RateLimitExceededException : ApiException(
    status = 429,
    code = "RATE_LIMITED",
    message = "Too many requests. Please try again later.",
)

interface RateLimitStore {
    suspend fun consume(key: String, limit: Int, windowSeconds: Long, now: Instant): Boolean
    suspend fun deleteExpired(before: Instant)
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

    override suspend fun deleteExpired(before: Instant) = synchronized(timestamps) {
        timestamps.values.forEach { entries -> while (entries.firstOrNull()?.isBefore(before) == true) entries.removeFirst() }
        timestamps.entries.removeIf { it.value.isEmpty() }
        Unit
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
    suspend fun recentConsecutiveFailures(emailHash: String, ipHash: String, since: Instant): List<LoginAttempt>
    suspend fun deleteBefore(before: Instant)
    suspend fun clearFailures(emailHash: String, ipHash: String)
}

class LocalLoginAttemptStore : LoginAttemptStore {
    private val attempts = ConcurrentHashMap<String, ArrayDeque<LoginAttempt>>()

    override suspend fun record(attempt: LoginAttempt) = synchronized(attempts) {
        attempts.computeIfAbsent("${attempt.emailHash}:${attempt.ipHash}") { ArrayDeque() }.addLast(attempt)
    }

    override suspend fun recentConsecutiveFailures(emailHash: String, ipHash: String, since: Instant): List<LoginAttempt> = synchronized(attempts) {
        val entries = attempts["$emailHash:$ipHash"] ?: return emptyList()
        while (entries.firstOrNull()?.attemptedAt?.isBefore(since) == true) entries.removeFirst()
        entries.toList().asReversed().takeWhile { !it.successful }
    }

    override suspend fun deleteBefore(before: Instant) = synchronized(attempts) {
        attempts.values.forEach { entries -> while (entries.firstOrNull()?.attemptedAt?.isBefore(before) == true) entries.removeFirst() }
        attempts.entries.removeIf { it.value.isEmpty() }
        Unit
    }

    override suspend fun clearFailures(emailHash: String, ipHash: String) = synchronized(attempts) {
        attempts["$emailHash:$ipHash"]?.removeIf { !it.successful }
        Unit
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
        rateLimits.deleteExpired(now.minusSeconds(config.rateLimitWindowSeconds))
        val ipKey = rateLimitKey(route, "ip", ipAddress.orEmpty())
        val emailKey = rateLimitKey(route, "email", normalizeEmail(email))
        if (!rateLimits.consume(ipKey, config.rateLimitMaxRequests, config.rateLimitWindowSeconds, now) ||
            (email != null && !rateLimits.consume(emailKey, config.rateLimitMaxRequests, config.rateLimitWindowSeconds, now))
        ) {
            throw RateLimitExceededException()
        }
    }

    suspend fun checkLoginLockout(email: String, ipAddress: String?) {
        val now = clock.instant()
        loginAttempts.deleteBefore(now.minusSeconds(config.loginLockoutMaxSeconds))
        val emailHash = hash(normalizeEmail(email))
        val ipHash = hash(ipAddress.orEmpty())
        val failures = loginAttempts.recentConsecutiveFailures(emailHash, ipHash, now.minusSeconds(config.loginLockoutMaxSeconds))
        if (failures.size < config.loginFailureThreshold) return
        val lockoutSeconds = (config.loginLockoutBaseSeconds * (1L shl (failures.size - config.loginFailureThreshold).coerceAtMost(30)))
            .coerceAtMost(config.loginLockoutMaxSeconds)
        if (failures.first().attemptedAt.plusSeconds(lockoutSeconds).isAfter(now)) throw RateLimitExceededException()
    }

    suspend fun recordLogin(email: String, ipAddress: String?, successful: Boolean) {
        val now = clock.instant()
        loginAttempts.deleteBefore(now.minusSeconds(config.loginLockoutMaxSeconds))
        val emailHash = hash(normalizeEmail(email))
        val ipHash = hash(ipAddress.orEmpty())
        if (successful) loginAttempts.clearFailures(emailHash, ipHash)
        else loginAttempts.record(LoginAttempt(emailHash, ipHash, now, successful))
        logger.info("authentication_event action=login outcome={}", if (successful) "success" else "failure")
    }

    private fun rateLimitKey(route: String, subject: String, value: String) = hash("$route:$subject:${hash(value)}")

    private fun normalizeEmail(email: String?) = email?.trim()?.lowercase().orEmpty()

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        val logger = LoggerFactory.getLogger(AuthenticationAbuseProtection::class.java)
    }
}
