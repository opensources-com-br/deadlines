package deadlines.identity.users

import deadlines.shared.database.DatabaseQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.SQLException
import java.time.ZoneOffset
import java.util.UUID

interface UserRepository {
    suspend fun create(user: User): User

    suspend fun findById(id: UUID): User?

    suspend fun findByEmail(email: String): User?

    suspend fun list(offset: Long, limit: Int): List<User>

    suspend fun count(): Long

    suspend fun update(user: User): User

    suspend fun markEmailVerified(id: UUID, verifiedAt: java.time.Instant): User?
}

data class UserCredentials(
    val user: User,
    val passwordHash: String,
)

interface UserCredentialsRepository {
    suspend fun create(user: User, passwordHash: String): User

    suspend fun findByEmail(email: String): UserCredentials?

    suspend fun updatePassword(userId: UUID, passwordHash: String, updatedAt: java.time.Instant): Boolean
}

class ExposedUserRepository(
    private val query: DatabaseQuery,
) : UserRepository {
    override suspend fun create(user: User): User =
        mapDuplicateEmail {
            query {
                UsersTable.insert {
                    it[id] = user.id
                    it[email] = user.email
                    it[status] = user.status.name.lowercase()
                    it[createdAt] = user.createdAt.atOffset(ZoneOffset.UTC)
                    it[updatedAt] = user.updatedAt.atOffset(ZoneOffset.UTC)
                }
                UserProfilesTable.insert {
                    it[userId] = user.id
                    it[firstName] = user.profile.firstName
                    it[lastName] = user.profile.lastName
                    it[avatarUrl] = user.profile.avatarUrl
                    it[phone] = user.profile.phone
                    it[createdAt] = user.createdAt.atOffset(ZoneOffset.UTC)
                    it[updatedAt] = user.updatedAt.atOffset(ZoneOffset.UTC)
                }
                user
            }
        }

    override suspend fun findById(id: UUID): User? =
        query {
            userQuery()
                .where { UsersTable.id eq id }
                .singleOrNull()
                ?.toUser()
        }

    override suspend fun findByEmail(email: String): User? =
        query {
            userQuery()
                .where { UsersTable.email.lowerCase() eq email.lowercase() }
                .singleOrNull()
                ?.toUser()
        }

    override suspend fun list(offset: Long, limit: Int): List<User> =
        query {
            userQuery()
                .orderBy(UsersTable.createdAt to SortOrder.DESC, UsersTable.id to SortOrder.ASC)
                .limit(limit)
                .offset(offset)
                .map { it.toUser() }
        }

    override suspend fun count(): Long = query { UsersTable.selectAll().count() }

    override suspend fun update(user: User): User =
        mapDuplicateEmail {
            query {
                UsersTable.update({ UsersTable.id eq user.id }) {
                    it[email] = user.email
                    it[status] = user.status.name.lowercase()
                    it[updatedAt] = user.updatedAt.atOffset(ZoneOffset.UTC)
                }
                UserProfilesTable.update({ UserProfilesTable.userId eq user.id }) {
                    it[firstName] = user.profile.firstName
                    it[lastName] = user.profile.lastName
                    it[avatarUrl] = user.profile.avatarUrl
                    it[phone] = user.profile.phone
                    it[updatedAt] = user.updatedAt.atOffset(ZoneOffset.UTC)
                }
                user
            }
        }

    override suspend fun markEmailVerified(id: UUID, verifiedAt: java.time.Instant): User? =
        query {
            UsersTable.update({ UsersTable.id eq id }) {
                it[status] = UserStatus.ACTIVE.name.lowercase()
                it[emailVerifiedAt] = verifiedAt.atOffset(ZoneOffset.UTC)
                it[updatedAt] = verifiedAt.atOffset(ZoneOffset.UTC)
            }
            userQuery().where { UsersTable.id eq id }.singleOrNull()?.toUser()
        }
}

class ExposedUserCredentialsRepository(
    private val query: DatabaseQuery,
) : UserCredentialsRepository {
    override suspend fun create(user: User, passwordHash: String): User =
        mapDuplicateEmail {
            query {
                UsersTable.insert {
                    it[id] = user.id
                    it[email] = user.email
                    it[status] = user.status.name.lowercase()
                    it[UsersTable.passwordHash] = passwordHash
                    it[createdAt] = user.createdAt.atOffset(ZoneOffset.UTC)
                    it[updatedAt] = user.updatedAt.atOffset(ZoneOffset.UTC)
                }
                UserProfilesTable.insert {
                    it[userId] = user.id
                    it[firstName] = user.profile.firstName
                    it[lastName] = user.profile.lastName
                    it[avatarUrl] = user.profile.avatarUrl
                    it[phone] = user.profile.phone
                    it[createdAt] = user.createdAt.atOffset(ZoneOffset.UTC)
                    it[updatedAt] = user.updatedAt.atOffset(ZoneOffset.UTC)
                }
                user
            }
        }

    override suspend fun findByEmail(email: String): UserCredentials? =
        query {
            userQuery()
                .where { UsersTable.email.lowerCase() eq email.lowercase() }
                .singleOrNull()
                ?.let { row ->
                    row[UsersTable.passwordHash]?.let { hash ->
                        UserCredentials(row.toUser(), hash)
                    }
                }
        }

    override suspend fun updatePassword(userId: UUID, passwordHash: String, updatedAt: java.time.Instant): Boolean =
        query {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.updatedAt] = updatedAt.atOffset(ZoneOffset.UTC)
            } == 1
        }
}

private suspend fun <T> mapDuplicateEmail(block: suspend () -> T): T =
    try {
        block()
    } catch (exception: Exception) {
        if (exception.hasSqlState(UNIQUE_VIOLATION_SQL_STATE)) {
            throw UserAlreadyExistsException()
        }
        throw exception
    }

private fun Throwable.hasSqlState(sqlState: String): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == sqlState }

private const val UNIQUE_VIOLATION_SQL_STATE = "23505"

private object UsersTable : Table("users") {
    val id = javaUUID("id")
    val email = varchar("email", 320)
    val status = varchar("status", 32)
    val passwordHash = varchar("password_hash", 100).nullable()
    val emailVerifiedAt = timestampWithTimeZone("email_verified_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

private object UserProfilesTable : Table("user_profiles") {
    val userId = javaUUID("user_id").references(UsersTable.id)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val avatarUrl = text("avatar_url").nullable()
    val phone = varchar("phone", 32).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId)
}

private fun userQuery() =
    (UsersTable innerJoin UserProfilesTable)
        .selectAll()

private fun org.jetbrains.exposed.v1.core.ResultRow.toUser() =
    User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        status = UserStatus.valueOf(this[UsersTable.status].uppercase()),
        profile =
            UserProfile(
                firstName = this[UserProfilesTable.firstName],
                lastName = this[UserProfilesTable.lastName],
                avatarUrl = this[UserProfilesTable.avatarUrl],
                phone = this[UserProfilesTable.phone],
            ),
        createdAt = this[UsersTable.createdAt].toInstant(),
        updatedAt = this[UsersTable.updatedAt].toInstant(),
        emailVerifiedAt = this[UsersTable.emailVerifiedAt]?.toInstant(),
    )
