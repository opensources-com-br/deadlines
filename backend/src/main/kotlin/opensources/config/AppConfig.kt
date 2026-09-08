package opensources.config

data class AppConfig(
    val http: HttpConfig,
    val database: DatabaseConfig,
    val auth: AuthConfig,
    val abuseProtection: AbuseProtectionConfig,
    val features: FeatureConfig,
    val email: EmailConfig,
    val product: ProductConfig,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): AppConfig =
            AppConfig(
                http = HttpConfig(
                    port = environment.positiveInt("PORT", default = 8080).also {
                        require(it <= 65_535) { "PORT must be between 1 and 65535" }
                    },
                    allowedCorsOrigins = environment.csv("CORS_ALLOWED_ORIGINS", default = listOf("http://localhost:3000")),
                    maxRequestBodyBytes = environment.positiveLong("HTTP_MAX_REQUEST_BODY_BYTES", default = 1_048_576),
                    trustedProxyAddresses = environment.csv("TRUSTED_PROXY_ADDRESSES", default = emptyList()),
                    requestReadTimeoutSeconds = environment.positiveInt("HTTP_REQUEST_READ_TIMEOUT_SECONDS", default = 30),
                    responseWriteTimeoutSeconds = environment.positiveInt("HTTP_RESPONSE_WRITE_TIMEOUT_SECONDS", default = 30),
                ),
                database = DatabaseConfig(
                    url = environment.required("DATABASE_URL"),
                    user = environment.required("DATABASE_USER"),
                    password = environment.required("DATABASE_PASSWORD"),
                    maximumPoolSize = environment.positiveInt("DATABASE_POOL_SIZE", default = 10),
                    migrationsLocation = environment["MIGRATIONS_LOCATION"] ?: "filesystem:../database/migrations",
                ),
                auth = AuthConfig(
                    jwtSecret = environment.required("JWT_SECRET").also {
                        require(it.length >= 32) { "JWT_SECRET must contain at least 32 characters" }
                    },
                    jwtIssuer = environment["JWT_ISSUER"] ?: "opensources",
                    jwtAudience = environment["JWT_AUDIENCE"] ?: "opensources-api",
                    accessTokenExpirationSeconds =
                        environment.positiveLong("JWT_ACCESS_EXPIRATION_SECONDS", default = 900),
                    refreshTokenExpirationSeconds =
                        environment.positiveLong("JWT_REFRESH_EXPIRATION_SECONDS", default = 2_592_000),
                ),
                abuseProtection = AbuseProtectionConfig(
                    rateLimitWindowSeconds = environment.positiveLong("AUTH_RATE_LIMIT_WINDOW_SECONDS", default = 60),
                    rateLimitMaxRequests = environment.positiveInt("AUTH_RATE_LIMIT_MAX_REQUESTS", default = 10),
                    loginFailureThreshold = environment.positiveInt("AUTH_LOGIN_FAILURE_THRESHOLD", default = 5),
                    loginLockoutBaseSeconds = environment.positiveLong("AUTH_LOGIN_LOCKOUT_BASE_SECONDS", default = 60),
                    loginLockoutMaxSeconds = environment.positiveLong("AUTH_LOGIN_LOCKOUT_MAX_SECONDS", default = 900),
                ),
                features = FeatureConfig(
                    billingEnabled = environment.boolean("FEATURE_BILLING_ENABLED", default = true),
                ),
                email =
                    EmailConfig(
                        provider =
                            EmailProvider.fromEnvironment(
                                environment["EMAIL_PROVIDER"],
                                environment["RESEND_API_KEY"]?.isNotBlank() == true,
                    ),
                        from =
                            environment["EMAIL_FROM"]?.takeIf(String::isNotBlank)
                                ?: environment["MAIL_FROM"]?.takeIf(String::isNotBlank)
                                ?: "no-reply@opensources.local",
                        resendApiKey = environment["RESEND_API_KEY"]?.takeIf(String::isNotBlank),
                        appBaseUrl =
                            environment["APP_BASE_URL"]?.takeIf(String::isNotBlank)
                                ?: environment["APP_WEB_URL"]?.takeIf(String::isNotBlank)
                                ?: "http://localhost:3000",
                    verificationExpirationSeconds =
                        environment.positiveLong("EMAIL_VERIFICATION_EXPIRATION_SECONDS", default = 86_400),
                    passwordResetExpirationSeconds =
                        environment.positiveLong("PASSWORD_RESET_EXPIRATION_SECONDS", default = 3_600),
                    invitationExpirationSeconds =
                        environment.positiveLong("INVITATION_EXPIRATION_SECONDS", default = 604_800),
                ),
                product = ProductConfig(
                    name = environment["PRODUCT_NAME"]?.takeIf(String::isNotBlank) ?: "opensources",
                    description = environment["PRODUCT_DESCRIPTION"]?.takeIf(String::isNotBlank) ?: "A reusable SaaS foundation",
                    logo = environment["PRODUCT_LOGO"]?.takeIf(String::isNotBlank) ?: "/opensources-mark.png",
                    defaultLocale = environment["PRODUCT_DEFAULT_LOCALE"]?.takeIf(String::isNotBlank) ?: "en",
                    defaultTimezone = environment["PRODUCT_DEFAULT_TIMEZONE"]?.takeIf(String::isNotBlank) ?: "UTC",
                    supportEmail = environment["PRODUCT_SUPPORT_EMAIL"]?.takeIf(String::isNotBlank) ?: "support@opensources.local",
                    applicationUrl = environment["PRODUCT_APPLICATION_URL"]?.takeIf(String::isNotBlank)
                        ?: environment["APP_BASE_URL"]?.takeIf(String::isNotBlank) ?: "http://localhost:3000",
                    enabledModules = environment.csv(
                        "PRODUCT_ENABLED_MODULES",
                        default = if (environment.boolean("FEATURE_BILLING_ENABLED", default = true)) listOf("billing") else emptyList(),
                    ).toSet(),
                ),
            )
    }
}

data class HttpConfig(
    val port: Int,
    val allowedCorsOrigins: List<String> = listOf("http://localhost:3000"),
    val maxRequestBodyBytes: Long = 1_048_576,
    val trustedProxyAddresses: List<String> = emptyList(),
    val requestReadTimeoutSeconds: Int = 30,
    val responseWriteTimeoutSeconds: Int = 30,
)

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maximumPoolSize: Int,
    val migrationsLocation: String,
)

data class AuthConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val accessTokenExpirationSeconds: Long,
    val refreshTokenExpirationSeconds: Long,
)

data class AbuseProtectionConfig(
    val rateLimitWindowSeconds: Long = 60,
    val rateLimitMaxRequests: Int = 10,
    val loginFailureThreshold: Int = 5,
    val loginLockoutBaseSeconds: Long = 60,
    val loginLockoutMaxSeconds: Long = 900,
) {
    init {
        require(loginLockoutMaxSeconds >= loginLockoutBaseSeconds) {
            "AUTH_LOGIN_LOCKOUT_MAX_SECONDS must be greater than or equal to AUTH_LOGIN_LOCKOUT_BASE_SECONDS"
        }
    }
}

data class FeatureConfig(
    val billingEnabled: Boolean,
)

data class EmailConfig(
    val from: String,
    val appBaseUrl: String,
    val verificationExpirationSeconds: Long,
    val passwordResetExpirationSeconds: Long,
    val invitationExpirationSeconds: Long = 604_800,
    val provider: EmailProvider = EmailProvider.LOGGING,
    val resendApiKey: String? = null,
) {
    init {
        require(provider != EmailProvider.RESEND || !resendApiKey.isNullOrBlank()) {
            "RESEND_API_KEY is required when EMAIL_PROVIDER is resend"
        }
    }
}

enum class EmailProvider {
    LOGGING,
    RESEND,
    ;

    companion object {
        fun fromEnvironment(value: String?, resendApiKeyPresent: Boolean): EmailProvider =
            when (value?.trim()?.uppercase()?.takeIf(String::isNotBlank) ?: if (resendApiKeyPresent) "RESEND" else "LOGGING") {
                "LOGGING" -> LOGGING
                "RESEND" -> RESEND
                else -> throw IllegalArgumentException("EMAIL_PROVIDER must be logging or resend")
            }
    }
}

private fun Map<String, String>.required(name: String): String =
    get(name)?.takeIf(String::isNotBlank)
        ?: throw IllegalArgumentException("Missing required environment variable: $name")

private fun Map<String, String>.positiveInt(name: String, default: Int): Int {
    val rawValue = get(name) ?: return default
    val value = rawValue.toIntOrNull()
        ?: throw IllegalArgumentException("$name must be an integer")
    require(value > 0) { "$name must be greater than zero" }
    return value
}

private fun Map<String, String>.positiveLong(name: String, default: Long): Long {
    val rawValue = get(name) ?: return default
    val value = rawValue.toLongOrNull()
        ?: throw IllegalArgumentException("$name must be an integer")
    require(value > 0) { "$name must be greater than zero" }
    return value
}

private fun Map<String, String>.boolean(name: String, default: Boolean): Boolean =
    get(name)?.trim()?.lowercase()?.let {
        when (it) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("$name must be true or false")
        }
    } ?: default

private fun Map<String, String>.csv(name: String, default: List<String>): List<String> =
    get(name)?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.distinct()?.takeIf(List<String>::isNotEmpty)
        ?: default
