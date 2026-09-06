package deadlines.organizations.invitations

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.invitationRoutes(service: InvitationOperations) {
    route("/api/v1/invitations") {
        get("/preview") {
            call.respond(service.preview(call.request.queryParameters["token"].orEmpty()))
        }

        authenticate("auth-jwt") {
            post("/accept") {
                val request = call.receive<AcceptInvitationRequest>()
                call.respond(service.accept(call.invitationUserId(), request.token))
            }

            get {
                call.respond(service.list(call.invitationUserId()))
            }
            post {
                val invitation = service.create(call.invitationUserId(), call.receive())
                call.response.header(HttpHeaders.Location, "/api/v1/invitations/${invitation.id}")
                call.respond(HttpStatusCode.Created, invitation)
            }
            route("/{invitationId}") {
                get {
                    call.respond(service.get(call.invitationUserId(), call.invitationResourceId()))
                }
                post("/resend") {
                    call.respond(service.resend(call.invitationUserId(), call.invitationResourceId()))
                }
                delete {
                    service.revoke(call.invitationUserId(), call.invitationResourceId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.invitationUserId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

private fun io.ktor.server.application.ApplicationCall.invitationResourceId(): UUID =
    parameters["invitationId"]
        ?.let { value -> runCatching { UUID.fromString(value) }.getOrNull() }
        ?: throw InvitationValidationException(mapOf("invitationId" to "must be a valid UUID"))
