package deadlines.organizations.members

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import java.util.UUID

fun Route.memberRoutes(service: MemberOperations) {
    authenticate("auth-jwt") {
        route("/api/v1/members") {
            get {
                call.respond(service.list(call.memberUserId()))
            }
            route("/{memberId}") {
                get {
                    call.respond(service.get(call.memberUserId(), call.memberResourceId()))
                }
                patch {
                    call.respond(service.updateRole(call.memberUserId(), call.memberResourceId(), call.receive()))
                }
                delete {
                    service.remove(call.memberUserId(), call.memberResourceId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.memberUserId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

private fun io.ktor.server.application.ApplicationCall.memberResourceId(): UUID =
    parameters["memberId"]
        ?.let { value -> runCatching { UUID.fromString(value) }.getOrNull() }
        ?: throw MemberValidationException(mapOf("memberId" to "must be a valid UUID"))
