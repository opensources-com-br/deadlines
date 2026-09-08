package opensources.identity.email

import opensources.identity.users.UserAlreadyExistsException
import opensources.shared.database.DatabaseQuery
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
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

class ExposedEmailChangeRepository(
    private val query: DatabaseQuery,
) : EmailChangeRepository {
    override suspend fun create(token: EmailChangeToken) {
        try {
            query {
                EmailChangeRequests.update({
                    (EmailChangeRequests.userId eq token.userId) and EmailChangeRequests.confirmedAt.isNull()
                }) { it[confirmedAt] = token.createdAt.atOffset(ZoneOffset.UTC) }
                EmailChangeRequests.insert {
                    it[id] = token.id
                    it[userId] = token.userId
                    it[newEmail] = token.newEmail
                    it[tokenHash] = token.tokenHash
                    it[expiresAt] = token.expiresAt.atOffset(ZoneOffset.UTC)
                    it[createdAt] = token.createdAt.atOffset(ZoneOffset.UTC)
                }
            }
        } catch (exception: Exception) {
            if (exception.hasUniqueViolation()) throw UserAlreadyExistsException()
            throw exception
        }
    }

    override suspend fun confirm(tokenHash: String, now: Instant): UUID? = query {
        val request = EmailChangeRequests.selectAll().where {
            (EmailChangeRequests.tokenHash eq tokenHash) and
                EmailChangeRequests.confirmedAt.isNull() and
                (EmailChangeRequests.expiresAt greater now.atOffset(ZoneOffset.UTC))
        }.singleOrNull() ?: return@query null
        val confirmed = EmailChangeRequests.update({
            (EmailChangeRequests.id eq request[EmailChangeRequests.id]) and EmailChangeRequests.confirmedAt.isNull()
        }) { it[confirmedAt] = now.atOffset(ZoneOffset.UTC) }
        if (confirmed != 1) return@query null
        Users.update({ Users.id eq request[EmailChangeRequests.userId] }) {
            it[email] = request[EmailChangeRequests.newEmail]
            it[updatedAt] = now.atOffset(ZoneOffset.UTC)
        }
        request[EmailChangeRequests.userId]
    }
}

private object EmailChangeRequests : Table("email_change_requests") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id")
    val newEmail = varchar("new_email", 320)
    val tokenHash = char("token_hash", 64)
    val expiresAt = timestampWithTimeZone("expires_at")
    val confirmedAt = timestampWithTimeZone("confirmed_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

private object Users : Table("users") {
    val id = javaUUID("id")
    val email = varchar("email", 320)
    val updatedAt = timestampWithTimeZone("updated_at")
}

private fun Throwable.hasUniqueViolation(): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == "23505" }
