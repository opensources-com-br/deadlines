package deadlines.application

import deadlines.identity.auth.AuthOperations
import deadlines.identity.auth.authRoutes
import deadlines.identity.users.UserService
import deadlines.identity.users.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
)

fun Application.configureRoutes(userService: UserService?, authService: AuthOperations?) {
    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }

        if (userService != null) {
            userRoutes(userService)
        }
        if (authService != null) {
            authRoutes(authService)
        }
    }
}
