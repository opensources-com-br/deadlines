package deadlines.subscriptions

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.util.UUID

fun Route.subscriptionRoutes(service: SubscriptionOperations) {
    authenticate("auth-jwt") {
        get("/api/v1/subscriptions/current") {
            call.respond(service.current(UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)))
        }
    }
}
