package deadlines.identity.email

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

data class EmailToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val createdAt: Instant,
)

interface EmailTokenRepository {
    suspend fun createVerification(token: EmailToken)
    suspend fun consumeVerification(tokenHash: String, now: Instant): UUID?
    suspend fun createPasswordReset(token: EmailToken)
    suspend fun consumePasswordReset(tokenHash: String, now: Instant): UUID?
}

class ExposedEmailTokenRepository(
    private val query: DatabaseQuery,
) : EmailTokenRepository {
    override suspend fun createVerification(token: EmailToken) {
        query {
        EmailVerifications.update({ (EmailVerifications.userId eq token.userId) and EmailVerifications.verifiedAt.isNull() }) {
            it[verifiedAt] = token.createdAt.atOffset(ZoneOffset.UTC)
        }
        EmailVerifications.insert {
            it[id] = token.id
            it[userId] = token.userId
            it[tokenHash] = token.tokenHash
            it[expiresAt] = token.expiresAt.atOffset(ZoneOffset.UTC)
            it[createdAt] = token.createdAt.atOffset(ZoneOffset.UTC)
        }
        }
    }

    override suspend fun consumeVerification(tokenHash: String, now: Instant): UUID? =
        query {
            val userId =
                EmailVerifications.selectAll()
                    .where {
                        (EmailVerifications.tokenHash eq tokenHash) and
                            EmailVerifications.verifiedAt.isNull() and
                            (EmailVerifications.expiresAt greater now.atOffset(ZoneOffset.UTC))
                    }
                    .singleOrNull()
                    ?.get(EmailVerifications.userId)
                    ?: return@query null
            val consumed =
                EmailVerifications.update({
                    (EmailVerifications.tokenHash eq tokenHash) and
                        EmailVerifications.verifiedAt.isNull() and
                        (EmailVerifications.expiresAt greater now.atOffset(ZoneOffset.UTC))
                }) { it[verifiedAt] = now.atOffset(ZoneOffset.UTC) }
            userId.takeIf { consumed == 1 }
        }

    override suspend fun createPasswordReset(token: EmailToken) {
        query {
        PasswordResets.update({ (PasswordResets.userId eq token.userId) and PasswordResets.usedAt.isNull() }) {
            it[usedAt] = token.createdAt.atOffset(ZoneOffset.UTC)
        }
        PasswordResets.insert {
            it[id] = token.id
            it[userId] = token.userId
            it[tokenHash] = token.tokenHash
            it[expiresAt] = token.expiresAt.atOffset(ZoneOffset.UTC)
            it[createdAt] = token.createdAt.atOffset(ZoneOffset.UTC)
        }
        }
    }

    override suspend fun consumePasswordReset(tokenHash: String, now: Instant): UUID? =
        query {
            val userId =
                PasswordResets.selectAll()
                    .where {
                        (PasswordResets.tokenHash eq tokenHash) and
                            PasswordResets.usedAt.isNull() and
                            (PasswordResets.expiresAt greater now.atOffset(ZoneOffset.UTC))
                    }
                    .singleOrNull()
                    ?.get(PasswordResets.userId)
                    ?: return@query null
            val consumed =
                PasswordResets.update({
                    (PasswordResets.tokenHash eq tokenHash) and
                        PasswordResets.usedAt.isNull() and
                        (PasswordResets.expiresAt greater now.atOffset(ZoneOffset.UTC))
                }) { it[usedAt] = now.atOffset(ZoneOffset.UTC) }
            userId.takeIf { consumed == 1 }
        }
}

private object EmailVerifications : Table("email_verifications") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id")
    val tokenHash = char("token_hash", 64)
    val expiresAt = timestampWithTimeZone("expires_at")
    val verifiedAt = timestampWithTimeZone("verified_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

private object PasswordResets : Table("password_resets") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id")
    val tokenHash = char("token_hash", 64)
    val expiresAt = timestampWithTimeZone("expires_at")
    val usedAt = timestampWithTimeZone("used_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}
