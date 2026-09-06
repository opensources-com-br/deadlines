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
        assertEquals("http://localhost:3000", config.email.appBaseUrl)
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
                        "EMAIL_FROM" to "identity@example.com",
                        "APP_BASE_URL" to "https://app.example.com",
                        "EMAIL_VERIFICATION_EXPIRATION_SECONDS" to "1200",
                        "PASSWORD_RESET_EXPIRATION_SECONDS" to "300",
                    ),
            )

        assertEquals(9090, config.http.port)
        assertEquals(20, config.database.maximumPoolSize)
        assertEquals("filesystem:/database/migrations", config.database.migrationsLocation)
        assertEquals("test-issuer", config.auth.jwtIssuer)
        assertEquals("test-audience", config.auth.jwtAudience)
        assertEquals(300, config.auth.accessTokenExpirationSeconds)
        assertEquals(600, config.auth.refreshTokenExpirationSeconds)
        assertEquals("identity@example.com", config.email.from)
        assertEquals("https://app.example.com", config.email.appBaseUrl)
        assertEquals(1200, config.email.verificationExpirationSeconds)
        assertEquals(300, config.email.passwordResetExpirationSeconds)
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
