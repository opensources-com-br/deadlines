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
            "JWT_SECRET" to "a-local-test-secret-with-32-characters",
        )

    @Test
    fun `loads required values and safe defaults`() {
        val config = AppConfig.fromEnvironment(requiredEnvironment)

        assertEquals(8080, config.http.port)
        assertEquals(10, config.database.maximumPoolSize)
        assertEquals("filesystem:../database/migrations", config.database.migrationsLocation)
        assertEquals(900, config.auth.accessTokenExpirationSeconds)
        assertEquals(2_592_000, config.auth.refreshTokenExpirationSeconds)
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
                        "JWT_ISSUER" to "test-issuer",
                        "JWT_AUDIENCE" to "test-audience",
                        "JWT_ACCESS_EXPIRATION_SECONDS" to "300",
                        "JWT_REFRESH_EXPIRATION_SECONDS" to "600",
                    ),
            )

        assertEquals(9090, config.http.port)
        assertEquals(20, config.database.maximumPoolSize)
        assertEquals("filesystem:/database/migrations", config.database.migrationsLocation)
        assertEquals("test-issuer", config.auth.jwtIssuer)
        assertEquals("test-audience", config.auth.jwtAudience)
        assertEquals(300, config.auth.accessTokenExpirationSeconds)
        assertEquals(600, config.auth.refreshTokenExpirationSeconds)
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

    @Test
    fun `rejects a short JWT secret`() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.fromEnvironment(requiredEnvironment + ("JWT_SECRET" to "too-short"))
        }
    }
}
