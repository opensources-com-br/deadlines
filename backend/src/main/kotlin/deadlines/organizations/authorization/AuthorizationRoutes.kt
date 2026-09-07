package deadlines.organizations.authorization

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.util.UUID

fun Route.authorizationRoutes(service: AuthorizationOperations) {
    authenticate("auth-jwt") {
        get("/api/v1/users/me/authorization") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            call.respond(service.context(userId).toResponse())
        }
    }
}
