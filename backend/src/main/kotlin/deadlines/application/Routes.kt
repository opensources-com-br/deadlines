package deadlines.application

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
)

fun Application.configureRoutes() {
    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }
    }
}
