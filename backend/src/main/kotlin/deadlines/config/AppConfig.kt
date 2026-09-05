package deadlines.config

data class AppConfig(
    val http: HttpConfig,
    val database: DatabaseConfig,
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
