package opensources.identity.auth

import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import opensources.shared.database.DatabaseQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere

class ExposedLoginAttemptStore(
    private val query: DatabaseQuery,
) : LoginAttemptStore {
    override suspend fun deleteBefore(before: Instant) {
        query { AuthenticationLoginAttempts.deleteWhere { attemptedAt less before.atOffset(ZoneOffset.UTC) } }
    }

    override suspend fun clearFailures(emailHash: String, ipHash: String) {
        query {
            AuthenticationLoginAttempts.deleteWhere {
                (AuthenticationLoginAttempts.emailHash eq emailHash) and
                    (AuthenticationLoginAttempts.ipHash eq ipHash) and
                    (AuthenticationLoginAttempts.successful eq false)
            }
        }
    }

    override suspend fun record(attempt: LoginAttempt) {
        query {
            AuthenticationLoginAttempts.insert {
                it[id] = UUID.randomUUID()
                it[emailHash] = attempt.emailHash
                it[ipHash] = attempt.ipHash
                it[attemptedAt] = attempt.attemptedAt.atOffset(ZoneOffset.UTC)
                it[successful] = attempt.successful
            }
        }
    }

    override suspend fun recentConsecutiveFailures(emailHash: String, ipHash: String, since: Instant): List<LoginAttempt> = query {
        AuthenticationLoginAttempts.selectAll().where {
            (AuthenticationLoginAttempts.emailHash eq emailHash) and
                (AuthenticationLoginAttempts.ipHash eq ipHash) and
                (AuthenticationLoginAttempts.attemptedAt greaterEq since.atOffset(ZoneOffset.UTC))
        }.orderBy(AuthenticationLoginAttempts.attemptedAt to SortOrder.DESC)
            .map {
                LoginAttempt(
                    it[AuthenticationLoginAttempts.emailHash], it[AuthenticationLoginAttempts.ipHash],
                    it[AuthenticationLoginAttempts.attemptedAt].toInstant(), it[AuthenticationLoginAttempts.successful],
                )
            }.takeWhile { !it.successful }
    }
}

private object AuthenticationLoginAttempts : Table("authentication_login_attempts") {
    val id = javaUUID("id")
    val emailHash = char("email_hash", 64)
    val ipHash = char("ip_hash", 64)
    val attemptedAt = timestampWithTimeZone("attempted_at")
    val successful = bool("successful")
    override val primaryKey = PrimaryKey(id)
}
