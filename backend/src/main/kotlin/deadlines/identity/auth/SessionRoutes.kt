package deadlines.identity.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.sessionRoutes(service: SessionService) {
    authenticate("auth-jwt") {
        route("/api/v1/sessions") {
            get {
                call.respond(service.list(call.userId(), call.sessionId()))
            }

            post("/revoke-all") {
                service.revokeAll(call.userId())
                call.respond(HttpStatusCode.NoContent)
            }

            delete("/{sessionId}") {
                service.revoke(call.userId(), call.requestedSessionId())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

private fun io.ktor.server.application.ApplicationCall.sessionId(): UUID? =
    principal<JWTPrincipal>()!!.payload.getClaim("sid").asString()?.let { raw ->
        runCatching { UUID.fromString(raw) }.getOrNull()
    }

private fun io.ktor.server.application.ApplicationCall.requestedSessionId(): UUID =
    parameters["sessionId"]
        ?.let { raw -> runCatching { UUID.fromString(raw) }.getOrNull() }
        ?: throw SessionValidationException()
