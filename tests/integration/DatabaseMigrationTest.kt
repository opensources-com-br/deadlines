package deadlines.integration

import deadlines.config.DatabaseConfig
import deadlines.shared.database.DatabaseFactory
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {
    @Test
    fun `startup applies the baseline migration`() {
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
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '001' AND success",
                ).use { statement ->
                    statement.executeQuery().use { result ->
                        result.next()
                        assertEquals(1, result.getInt(1))
                    }
                }
            }
        }
    }

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
