package opensources.identity.preferences

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import java.util.UUID

fun Route.userPreferenceRoutes(service: UserPreferenceOperations) {
    authenticate("auth-jwt") {
        route("/api/v1/users/me/preferences") {
            get { call.respond(service.get(call.userId())) }
            patch { call.respond(service.update(call.userId(), call.receive())) }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)
