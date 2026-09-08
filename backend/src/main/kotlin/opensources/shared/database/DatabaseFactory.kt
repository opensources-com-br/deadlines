package opensources.shared.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import opensources.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

class DatabaseHandle internal constructor(
    val database: Database,
    private val dataSource: HikariDataSource,
) : AutoCloseable {
    fun isReady(): Boolean =
        runCatching {
            dataSource.connection.use { connection -> connection.isValid(2) }
        }.getOrDefault(false)

    override fun close() = dataSource.close()
}

object DatabaseFactory {
    fun open(config: DatabaseConfig): DatabaseHandle {
        val dataSource = HikariDataSource(config.toHikariConfig())

        return try {
            migrate(dataSource, config.migrationLocations)
            DatabaseHandle(
                database = Database.connect(dataSource),
                dataSource = dataSource,
            )
        } catch (error: Throwable) {
            dataSource.close()
            throw error
        }
    }

    private fun migrate(dataSource: HikariDataSource, locations: List<String>) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations(*locations.toTypedArray())
            .load()
            .migrate()
    }
}

private fun DatabaseConfig.toHikariConfig() =
    HikariConfig().apply {
        jdbcUrl = url
        username = user
        password = this@toHikariConfig.password
        maximumPoolSize = this@toHikariConfig.maximumPoolSize
        minimumIdle = 1
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_READ_COMMITTED"
        validate()
    }
