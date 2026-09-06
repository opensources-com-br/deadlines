package deadlines.identity.auth

import deadlines.shared.database.DatabaseQuery
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

data class Session(
    val id: UUID,
    val userId: UUID,
    val refreshTokenHash: String,
    val userAgent: String?,
    val ipAddress: String?,
    val expiresAt: Instant,
    val createdAt: Instant,
)

interface SessionRepository {
    suspend fun create(session: Session)

    suspend fun findActive(refreshTokenHash: String, now: Instant): Session?

    suspend fun rotate(currentHash: String, replacement: Session, now: Instant): Boolean

    suspend fun revoke(refreshTokenHash: String, now: Instant): Boolean

    suspend fun revokeAll(userId: UUID, now: Instant): Int
}

class ExposedSessionRepository(
    private val query: DatabaseQuery,
) : SessionRepository {
    override suspend fun create(session: Session) {
        query { insert(session) }
    }

    override suspend fun findActive(refreshTokenHash: String, now: Instant): Session? =
        query {
            SessionsTable.selectAll()
                .where {
                    (SessionsTable.refreshTokenHash eq refreshTokenHash) and
                        SessionsTable.revokedAt.isNull() and
                        (SessionsTable.expiresAt greater now.atOffset(ZoneOffset.UTC))
                }
                .singleOrNull()
                ?.toSession()
        }

    override suspend fun rotate(currentHash: String, replacement: Session, now: Instant): Boolean =
        query {
            val revoked = revokeActive(currentHash, now)
            if (revoked == 1) insert(replacement)
            revoked == 1
        }

    override suspend fun revoke(refreshTokenHash: String, now: Instant): Boolean =
        query { revokeActive(refreshTokenHash, now) == 1 }

    override suspend fun revokeAll(userId: UUID, now: Instant): Int =
        query {
            SessionsTable.update({ (SessionsTable.userId eq userId) and SessionsTable.revokedAt.isNull() }) {
                it[revokedAt] = now.atOffset(ZoneOffset.UTC)
            }
        }

    private fun revokeActive(refreshTokenHash: String, now: Instant): Int =
        SessionsTable.update({
            (SessionsTable.refreshTokenHash eq refreshTokenHash) and SessionsTable.revokedAt.isNull()
        }) {
            it[revokedAt] = now.atOffset(ZoneOffset.UTC)
        }

    private fun insert(session: Session) {
        SessionsTable.insert {
            it[id] = session.id
            it[userId] = session.userId
            it[refreshTokenHash] = session.refreshTokenHash
            it[userAgent] = session.userAgent
            it[ipAddress] = session.ipAddress
            it[expiresAt] = session.expiresAt.atOffset(ZoneOffset.UTC)
            it[createdAt] = session.createdAt.atOffset(ZoneOffset.UTC)
        }
    }
}

private object SessionsTable : Table("sessions") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id")
    val refreshTokenHash = char("refresh_token_hash", 64)
    val userAgent = text("user_agent").nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdAt = timestampWithTimeZone("created_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

private fun org.jetbrains.exposed.v1.core.ResultRow.toSession() =
    Session(
        id = this[SessionsTable.id],
        userId = this[SessionsTable.userId],
        refreshTokenHash = this[SessionsTable.refreshTokenHash],
        userAgent = this[SessionsTable.userAgent],
        ipAddress = this[SessionsTable.ipAddress],
        expiresAt = this[SessionsTable.expiresAt].toInstant(),
        createdAt = this[SessionsTable.createdAt].toInstant(),
    )
