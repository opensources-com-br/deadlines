package deadlines.application

import deadlines.config.AppConfig
import deadlines.identity.users.ExposedUserRepository
import deadlines.identity.users.UserService
import deadlines.shared.database.DatabaseFactory
import deadlines.shared.database.DatabaseQuery
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = AppConfig.fromEnvironment()

    DatabaseFactory.open(config.database).use { database ->
        val userService = UserService(ExposedUserRepository(DatabaseQuery(database.database)))

        embeddedServer(Netty, port = config.http.port) {
            module(userService)
        }.start(wait = true)
    }
}

fun Application.module(userService: UserService? = null) {
    configurePlugins()
    configureRoutes(userService)
}
