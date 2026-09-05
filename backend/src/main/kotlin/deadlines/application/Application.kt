package deadlines.application

import deadlines.config.AppConfig
import deadlines.shared.database.DatabaseFactory
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = AppConfig.fromEnvironment()

    DatabaseFactory.open(config.database).use {
        embeddedServer(Netty, port = config.http.port) {
            module()
        }.start(wait = true)
    }
}

fun Application.module() {
    configurePlugins()
    configureRoutes()
}
