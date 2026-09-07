package deadlines.identity.preferences

import deadlines.shared.database.DatabaseQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

interface UserPreferenceRepository {
    suspend fun findByUserId(userId: UUID): UserPreference?
    suspend fun update(userId: UUID, locale: String, timezone: String, theme: String, updatedAt: Instant): UserPreference?
}

class ExposedUserPreferenceRepository(private val query: DatabaseQuery) : UserPreferenceRepository {
    override suspend fun findByUserId(userId: UUID): UserPreference? = query {
        UserPreferencesTable.selectAll().where { UserPreferencesTable.userId eq userId }.singleOrNull()?.toPreference()
    }

    override suspend fun update(userId: UUID, locale: String, timezone: String, theme: String, updatedAt: Instant): UserPreference? = query {
        UserPreferencesTable.update({ UserPreferencesTable.userId eq userId }) {
            it[UserPreferencesTable.locale] = locale
            it[UserPreferencesTable.timezone] = timezone
            it[UserPreferencesTable.theme] = theme
            it[UserPreferencesTable.updatedAt] = updatedAt.atOffset(ZoneOffset.UTC)
        }
        UserPreferencesTable.selectAll().where { UserPreferencesTable.userId eq userId }.singleOrNull()?.toPreference()
    }
}

object UserPreferencesTable : Table("user_preferences") {
    val userId = javaUUID("user_id")
    val locale = varchar("locale", 16)
    val timezone = varchar("timezone", 64)
    val theme = varchar("theme", 16)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(userId)
}

private fun ResultRow.toPreference() = UserPreference(
    userId = this[UserPreferencesTable.userId],
    locale = this[UserPreferencesTable.locale],
    timezone = this[UserPreferencesTable.timezone],
    theme = this[UserPreferencesTable.theme],
    createdAt = this[UserPreferencesTable.createdAt].toInstant(),
    updatedAt = this[UserPreferencesTable.updatedAt].toInstant(),
)
