package deadlines.application

import deadlines.config.AppConfig
import deadlines.identity.auth.AuthOperations
import deadlines.identity.auth.AuthService
import deadlines.identity.auth.BcryptPasswordHasher
import deadlines.identity.auth.ExposedSessionRepository
import deadlines.identity.auth.TokenService
import deadlines.identity.users.ExposedUserCredentialsRepository
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
        val query = DatabaseQuery(database.database)
        val userRepository = ExposedUserRepository(query)
        val tokenService = TokenService(config.auth)
        val userService = UserService(userRepository)
        val authService =
            AuthService(
                ExposedUserCredentialsRepository(query),
                userRepository,
                ExposedSessionRepository(query),
                BcryptPasswordHasher(),
                tokenService,
            )

        embeddedServer(Netty, port = config.http.port) {
            module(userService, authService, tokenService)
        }.start(wait = true)
    }
}

fun Application.module(
    userService: UserService? = null,
    authService: AuthOperations? = null,
    tokenService: TokenService? = null,
) {
    configurePlugins(tokenService)
    configureRoutes(userService, authService)
}
