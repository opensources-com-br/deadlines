package opensources.identity.users

import opensources.shared.database.DatabaseQuery
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.update

class ExposedAccountLifecycleRepository(
    private val query: DatabaseQuery,
) : AccountLifecycleRepository {
    override suspend fun deactivate(userId: UUID, disabledAt: Instant): Boolean = query {
        val timestamp = disabledAt.atOffset(ZoneOffset.UTC)
        val updated = LifecycleUsersTable.update({
            (LifecycleUsersTable.id eq userId) and (LifecycleUsersTable.status eq "active")
        }) {
            it[status] = "disabled"
            it[LifecycleUsersTable.disabledAt] = timestamp
            it[updatedAt] = timestamp
        }
        if (updated != 1) return@query false
        revokeSessions(userId, timestamp)
        true
    }

    override suspend fun delete(userId: UUID, deletedAt: Instant): Boolean = query {
        val timestamp = deletedAt.atOffset(ZoneOffset.UTC)
        val updated = LifecycleUsersTable.update({
            (LifecycleUsersTable.id eq userId) and (LifecycleUsersTable.status eq "active")
        }) {
            it[email] = "deleted-$userId@opensources.invalid"
            it[status] = "deleted"
            it[passwordHash] = null
            it[disabledAt] = timestamp
            it[LifecycleUsersTable.deletedAt] = timestamp
            it[updatedAt] = timestamp
        }
        if (updated != 1) return@query false

        LifecycleProfilesTable.update({ LifecycleProfilesTable.userId eq userId }) {
            it[firstName] = "Deleted"
            it[lastName] = "User"
            it[avatarUrl] = null
            it[phone] = null
            it[updatedAt] = timestamp
        }
        LifecycleMembershipsTable.update({
            (LifecycleMembershipsTable.userId eq userId) and
                (LifecycleMembershipsTable.status inList listOf("active", "suspended"))
        }) {
            it[status] = "removed"
            it[removedAt] = timestamp
        }
        LifecyclePreferencesTable.deleteWhere { LifecyclePreferencesTable.userId eq userId }
        LifecycleEmailVerificationsTable.deleteWhere { LifecycleEmailVerificationsTable.userId eq userId }
        LifecyclePasswordResetsTable.deleteWhere { LifecyclePasswordResetsTable.userId eq userId }
        revokeSessions(userId, timestamp)
        true
    }

    private fun revokeSessions(userId: UUID, timestamp: java.time.OffsetDateTime) {
        LifecycleSessionsTable.update({
            (LifecycleSessionsTable.userId eq userId) and LifecycleSessionsTable.revokedAt.isNull()
        }) {
            it[revokedAt] = timestamp
        }
    }
}

private object LifecycleUsersTable : Table("users") {
    val id = javaUUID("id")
    val email = varchar("email", 320)
    val status = varchar("status", 32)
    val passwordHash = varchar("password_hash", 100).nullable()
    val disabledAt = timestampWithTimeZone("disabled_at").nullable()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
}

private object LifecycleProfilesTable : Table("user_profiles") {
    val userId = javaUUID("user_id")
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val avatarUrl = text("avatar_url").nullable()
    val phone = varchar("phone", 32).nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
}

private object LifecycleMembershipsTable : Table("organization_memberships") {
    val userId = javaUUID("user_id")
    val status = varchar("status", 32)
    val removedAt = timestampWithTimeZone("removed_at").nullable()
}

private object LifecyclePreferencesTable : Table("user_preferences") {
    val userId = javaUUID("user_id")
}

private object LifecycleEmailVerificationsTable : Table("email_verifications") {
    val userId = javaUUID("user_id")
}

private object LifecyclePasswordResetsTable : Table("password_resets") {
    val userId = javaUUID("user_id")
}

private object LifecycleSessionsTable : Table("sessions") {
    val userId = javaUUID("user_id")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
}
