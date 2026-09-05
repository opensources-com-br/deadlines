package deadlines.config

data class AppConfig(
    val http: HttpConfig,
    val database: DatabaseConfig,
    val auth: AuthConfig,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): AppConfig =
            AppConfig(
                http = HttpConfig(
                    port = environment.positiveInt("PORT", default = 8080).also {
                        require(it <= 65_535) { "PORT must be between 1 and 65535" }
                    },
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
                    jwtIssuer = environment["JWT_ISSUER"] ?: "deadlines",
                    jwtAudience = environment["JWT_AUDIENCE"] ?: "deadlines-api",
                    accessTokenExpirationSeconds =
                        environment.positiveLong("JWT_ACCESS_EXPIRATION_SECONDS", default = 900),
                    refreshTokenExpirationSeconds =
                        environment.positiveLong("JWT_REFRESH_EXPIRATION_SECONDS", default = 2_592_000),
                ),
            )
    }
}

data class HttpConfig(
    val port: Int,
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
