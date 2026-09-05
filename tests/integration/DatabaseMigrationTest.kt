package deadlines.integration

import deadlines.config.DatabaseConfig
import deadlines.identity.auth.ExposedSessionRepository
import deadlines.identity.auth.Session
import deadlines.identity.users.ExposedUserRepository
import deadlines.identity.users.ExposedUserCredentialsRepository
import deadlines.identity.users.User
import deadlines.identity.users.UserAlreadyExistsException
import deadlines.identity.users.UserProfile
import deadlines.identity.users.UserStatus
import deadlines.shared.database.DatabaseQuery
import deadlines.shared.database.DatabaseFactory
import kotlinx.coroutines.test.runTest
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {
    @Test
    fun `startup applies every migration`() {
        val config =
            DatabaseConfig(
                url = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password,
                maximumPoolSize = 2,
                migrationsLocation = migrationLocation(),
            )

        DatabaseFactory.open(config).use {
            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                connection.prepareStatement(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                ).use { statement ->
                    statement.executeQuery().use { result ->
                        result.next()
                        assertEquals(3, result.getInt(1))
                    }
                }
            }
        }
    }

    @Test
    fun `user repository persists and reads a complete user`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val repository = ExposedUserRepository(DatabaseQuery(database.database))
                val now = Instant.parse("2026-09-05T12:00:00Z")
                val user =
                    User(
                        id = UUID.randomUUID(),
                        email = "repository@example.com",
                        status = UserStatus.ACTIVE,
                        profile = UserProfile("Repo", "Test", null, "+5511999999999"),
                        createdAt = now,
                        updatedAt = now,
                    )

                repository.create(user)
                val found = repository.findByEmail("REPOSITORY@EXAMPLE.COM")

                assertEquals(user, found)
                assertEquals(1, repository.count())
                assertEquals(listOf(user), repository.list(offset = 0, limit = 20))

                val updated = user.copy(status = UserStatus.DISABLED, updatedAt = now.plusSeconds(60))
                repository.update(updated)
                assertEquals(updated, repository.findById(user.id))
            }
        }

    @Test
    fun `database uniqueness violation is exposed as a user conflict`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val repository = ExposedUserRepository(DatabaseQuery(database.database))
                val now = Instant.parse("2026-09-05T12:00:00Z")
                val first =
                    User(
                        id = UUID.randomUUID(),
                        email = "unique@example.com",
                        status = UserStatus.PENDING,
                        profile = UserProfile("First", "User", null, null),
                        createdAt = now,
                        updatedAt = now,
                    )
                val duplicate =
                    first.copy(
                        id = UUID.randomUUID(),
                        email = "UNIQUE@EXAMPLE.COM",
                    )

                repository.create(first)

                assertFailsWith<UserAlreadyExistsException> {
                    repository.create(duplicate)
                }
            }
        }

    @Test
    fun `credentials repository persists a password hash without exposing it on the user`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val repository = ExposedUserCredentialsRepository(DatabaseQuery(database.database))
                val now = Instant.parse("2026-09-05T12:00:00Z")
                val user =
                    User(
                        id = UUID.randomUUID(),
                        email = "credentials@example.com",
                        status = UserStatus.ACTIVE,
                        profile = UserProfile("Credential", "Test", null, null),
                        createdAt = now,
                        updatedAt = now,
                    )

                repository.create(user, "a-password-hash")
                val credentials = repository.findByEmail("CREDENTIALS@EXAMPLE.COM")

                assertEquals(user, credentials?.user)
                assertEquals("a-password-hash", credentials?.passwordHash)
            }
        }

    @Test
    fun `session repository rotates a refresh token atomically`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val sessions = ExposedSessionRepository(query)
                val now = Instant.now()
                val user =
                    User(
                        id = UUID.randomUUID(),
                        email = "session-${UUID.randomUUID()}@example.com",
                        status = UserStatus.ACTIVE,
                        profile = UserProfile("Session", "Test", null, null),
                        createdAt = now,
                        updatedAt = now,
                    )
                val first = Session(UUID.randomUUID(), user.id, "a".repeat(64), null, null, now.plusSeconds(60), now)
                val replacement =
                    Session(UUID.randomUUID(), user.id, "b".repeat(64), null, null, now.plusSeconds(120), now)

                users.create(user)
                sessions.create(first)

                assertEquals(first, sessions.findActive(first.refreshTokenHash, now))
                assertEquals(true, sessions.rotate(first.refreshTokenHash, replacement, now))
                assertNull(sessions.findActive(first.refreshTokenHash, now))
                assertEquals(replacement, sessions.findActive(replacement.refreshTokenHash, now))
                assertEquals(false, sessions.rotate(first.refreshTokenHash, replacement, now))
            }
        }

    private fun databaseConfig() =
        DatabaseConfig(
            url = postgres.jdbcUrl,
            user = postgres.username,
            password = postgres.password,
            maximumPoolSize = 2,
            migrationsLocation = migrationLocation(),
        )

    private fun migrationLocation(): String {
        val migrations = Path.of("../database/migrations").toAbsolutePath().normalize()
        return "filesystem:$migrations"
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
}
