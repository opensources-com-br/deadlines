package deadlines.organizations.access

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
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.permissionRoutes(service: PermissionOperations) {
    authenticate("auth-jwt") {
        route("/api/v1/permissions") {
            get {
                call.respond(service.list(call.accessUserId()))
            }
            post {
                val permission = service.create(call.accessUserId(), call.receive())
                call.response.header(HttpHeaders.Location, "/api/v1/permissions/${permission.id}")
                call.respond(HttpStatusCode.Created, permission)
            }
            route("/{permissionId}") {
                get {
                    call.respond(service.get(call.accessUserId(), call.accessResourceId("permissionId")))
                }
                patch {
                    call.respond(
                        service.update(call.accessUserId(), call.accessResourceId("permissionId"), call.receive()),
                    )
                }
                delete {
                    service.delete(call.accessUserId(), call.accessResourceId("permissionId"))
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

internal fun io.ktor.server.application.ApplicationCall.accessUserId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

internal fun io.ktor.server.application.ApplicationCall.accessResourceId(parameter: String): UUID =
    parameters[parameter]
        ?.let { value -> runCatching { UUID.fromString(value) }.getOrNull() }
        ?: throw AccessValidationException(mapOf(parameter to "must be a valid UUID"))
