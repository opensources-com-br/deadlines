package opensources.identity.auth

import java.time.Instant
import java.time.ZoneOffset
import opensources.shared.database.DatabaseQuery
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ExposedRateLimitStore(
    private val query: DatabaseQuery,
) : RateLimitStore {
    override suspend fun consume(key: String, limit: Int, windowSeconds: Long, now: Instant): Boolean = query {
        val current = AuthenticationRateLimits.selectAll()
            .where { AuthenticationRateLimits.keyHash eq key }
            .singleOrNull()
        if (current == null || !current[AuthenticationRateLimits.windowStartedAt].toInstant().plusSeconds(windowSeconds).isAfter(now)) {
            if (current == null) {
                AuthenticationRateLimits.insert {
                    it[keyHash] = key
                    it[windowStartedAt] = now.atOffset(ZoneOffset.UTC)
                    it[requestCount] = 1
                    it[updatedAt] = now.atOffset(ZoneOffset.UTC)
                }
            } else {
                AuthenticationRateLimits.update({ AuthenticationRateLimits.keyHash eq key }) {
                    it[windowStartedAt] = now.atOffset(ZoneOffset.UTC)
                    it[requestCount] = 1
                    it[updatedAt] = now.atOffset(ZoneOffset.UTC)
                }
            }
            return@query true
        }
        val count = current[AuthenticationRateLimits.requestCount]
        if (count >= limit) return@query false
        AuthenticationRateLimits.update({ AuthenticationRateLimits.keyHash eq key }) {
            it[requestCount] = count + 1
            it[updatedAt] = now.atOffset(ZoneOffset.UTC)
        }
        true
    }
}

private object AuthenticationRateLimits : Table("authentication_rate_limits") {
    val keyHash = char("key_hash", 64)
    val windowStartedAt = timestampWithTimeZone("window_started_at")
    val requestCount = integer("request_count")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(keyHash)
}
