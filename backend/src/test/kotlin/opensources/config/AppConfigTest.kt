package opensources.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppConfigTest {
    private val requiredEnvironment =
        mapOf(
            "DATABASE_URL" to "jdbc:postgresql://localhost:5432/opensources",
            "DATABASE_USER" to "opensources",
            "DATABASE_PASSWORD" to "secret",
            "JWT_SECRET" to "a-local-test-secret-with-32-characters",
        )

    @Test
    fun `loads required values and safe defaults`() {
        val config = AppConfig.fromEnvironment(requiredEnvironment)

        assertEquals(8080, config.http.port)
        assertEquals(listOf("http://localhost:3000"), config.http.allowedCorsOrigins)
        assertEquals(30, config.http.requestReadTimeoutSeconds)
        assertEquals(30, config.http.responseWriteTimeoutSeconds)
        assertEquals(10, config.database.maximumPoolSize)
        assertEquals("filesystem:../database/migrations/core", config.database.migrationsLocation)
        assertEquals(
            listOf("filesystem:../database/migrations/core", "filesystem:../database/migrations/billing"),
            config.database.migrationLocations,
        )
        assertEquals(900, config.auth.accessTokenExpirationSeconds)
        assertEquals(2_592_000, config.auth.refreshTokenExpirationSeconds)
        assertEquals(true, config.features.billingEnabled)
        assertEquals(EmailProvider.LOGGING, config.email.provider)
        assertEquals("http://localhost:3000", config.email.appBaseUrl)
        assertEquals("opensources", config.product.name)
        assertEquals(true, config.product.isModuleEnabled("billing"))
        assertEquals(604_800, config.email.invitationExpirationSeconds)
    }

    @Test
    fun `accepts explicit optional values`() {
        val config =
            AppConfig.fromEnvironment(
                requiredEnvironment +
                    mapOf(
                        "PORT" to "9090",
                        "CORS_ALLOWED_ORIGINS" to "https://app.example.com, https://admin.example.com",
                        "HTTP_REQUEST_READ_TIMEOUT_SECONDS" to "15",
                        "HTTP_RESPONSE_WRITE_TIMEOUT_SECONDS" to "20",
                        "DATABASE_POOL_SIZE" to "20",
                        "CORE_MIGRATIONS_LOCATION" to "filesystem:/database/migrations/core",
                        "BILLING_MIGRATIONS_LOCATION" to "filesystem:/database/migrations/billing",
                        "JWT_ISSUER" to "test-issuer",
                        "JWT_AUDIENCE" to "test-audience",
                        "JWT_ACCESS_EXPIRATION_SECONDS" to "300",
                        "JWT_REFRESH_EXPIRATION_SECONDS" to "600",
                        "EMAIL_PROVIDER" to "resend",
                        "EMAIL_FROM" to "identity@example.com",
                        "RESEND_API_KEY" to "re_test_key",
                        "APP_BASE_URL" to "https://app.example.com",
                        "EMAIL_VERIFICATION_EXPIRATION_SECONDS" to "1200",
                        "PASSWORD_RESET_EXPIRATION_SECONDS" to "300",
                        "INVITATION_EXPIRATION_SECONDS" to "600",
                        "FEATURE_BILLING_ENABLED" to "false",
                        "PRODUCT_NAME" to "Example",
                        "PRODUCT_ENABLED_MODULES" to "billing, reports, exports",
                    ),
            )

        assertEquals(9090, config.http.port)
        assertEquals(listOf("https://app.example.com", "https://admin.example.com"), config.http.allowedCorsOrigins)
        assertEquals(15, config.http.requestReadTimeoutSeconds)
        assertEquals(20, config.http.responseWriteTimeoutSeconds)
        assertEquals(20, config.database.maximumPoolSize)
        assertEquals("filesystem:/database/migrations/core", config.database.migrationsLocation)
        assertEquals(
            listOf("filesystem:/database/migrations/core", "filesystem:/database/migrations/billing"),
            config.database.migrationLocations,
        )
        assertEquals("test-issuer", config.auth.jwtIssuer)
        assertEquals("test-audience", config.auth.jwtAudience)
        assertEquals(300, config.auth.accessTokenExpirationSeconds)
        assertEquals(600, config.auth.refreshTokenExpirationSeconds)
        assertEquals(EmailProvider.RESEND, config.email.provider)
        assertEquals("identity@example.com", config.email.from)
        assertEquals("re_test_key", config.email.resendApiKey)
        assertEquals("https://app.example.com", config.email.appBaseUrl)
        assertEquals(1200, config.email.verificationExpirationSeconds)
        assertEquals(300, config.email.passwordResetExpirationSeconds)
        assertEquals(600, config.email.invitationExpirationSeconds)
        assertEquals(false, config.features.billingEnabled)
        assertEquals("Example", config.product.name)
        assertEquals(true, config.product.isModuleEnabled("billing"))
    }

    @Test
    fun `uses Resend when a local Resend key is configured`() {
        val config =
            AppConfig.fromEnvironment(
                requiredEnvironment +
                    mapOf(
                        "RESEND_API_KEY" to "re_test_key",
                        "MAIL_FROM" to "opensources <onboarding@resend.dev>",
                        "APP_WEB_URL" to "http://localhost:3000",
                    ),
            )

        assertEquals(EmailProvider.RESEND, config.email.provider)
        assertEquals("opensources <onboarding@resend.dev>", config.email.from)
    }

    @Test
    fun `treats a blank provider as absent`() {
        val config = AppConfig.fromEnvironment(requiredEnvironment + ("EMAIL_PROVIDER" to " "))

        assertEquals(EmailProvider.LOGGING, config.email.provider)
    }

    @Test
    fun `excludes billing migrations when the billing module is disabled`() {
        val config = AppConfig.fromEnvironment(requiredEnvironment + ("PRODUCT_ENABLED_MODULES" to "reports"))

        assertEquals(listOf("filesystem:../database/migrations/core"), config.database.migrationLocations)
        assertEquals(false, config.product.isModuleEnabled("billing"))
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

    @Test
    fun `rejects an invalid feature flag`() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.fromEnvironment(requiredEnvironment + ("FEATURE_BILLING_ENABLED" to "enabled"))
        }
    }
}
