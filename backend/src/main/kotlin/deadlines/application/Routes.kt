package deadlines.application

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

fun Application.configureRoutes(userService: UserService?) {
    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }

        if (userService != null) {
            userRoutes(userService)
        }
    }
}
