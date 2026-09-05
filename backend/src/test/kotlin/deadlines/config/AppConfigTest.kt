package deadlines.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppConfigTest {
    private val requiredEnvironment =
        mapOf(
            "DATABASE_URL" to "jdbc:postgresql://localhost:5432/deadlines",
            "DATABASE_USER" to "deadlines",
            "DATABASE_PASSWORD" to "secret",
        )

    @Test
    fun `loads required values and safe defaults`() {
        val config = AppConfig.fromEnvironment(requiredEnvironment)

        assertEquals(8080, config.http.port)
        assertEquals(10, config.database.maximumPoolSize)
        assertEquals("filesystem:../database/migrations", config.database.migrationsLocation)
    }

    @Test
    fun `accepts explicit optional values`() {
        val config =
            AppConfig.fromEnvironment(
                requiredEnvironment +
                    mapOf(
                        "PORT" to "9090",
                        "DATABASE_POOL_SIZE" to "20",
                        "MIGRATIONS_LOCATION" to "filesystem:/database/migrations",
                    ),
            )

        assertEquals(9090, config.http.port)
        assertEquals(20, config.database.maximumPoolSize)
        assertEquals("filesystem:/database/migrations", config.database.migrationsLocation)
    }

    @Test
    fun `rejects missing database credentials`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                AppConfig.fromEnvironment(emptyMap())
            }

        assertEquals("Missing required environment variable: DATABASE_URL", error.message)
    }

    @Test
    fun `rejects invalid port`() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.fromEnvironment(requiredEnvironment + ("PORT" to "70000"))
        }
    }
}
