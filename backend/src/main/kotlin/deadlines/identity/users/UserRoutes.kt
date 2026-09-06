package deadlines.identity.users

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

fun Route.userRoutes(service: UserService) {
    route("/api/v1/users") {
        authenticate("auth-jwt") {
            route("/me") {
                get {
                    call.respond(service.get(call.authenticatedUserId()).toResponse())
                }

                patch {
                    val user = service.updateOwnProfile(call.authenticatedUserId(), call.receive())
                    call.respond(user.toResponse())
                }
            }
        }

        post {
            val user = service.create(call.receive<CreateUserRequest>())
            call.response.header(HttpHeaders.Location, "/api/v1/users/${user.id}")
            call.respond(HttpStatusCode.Created, user.toResponse())
        }

        get {
            val page = call.queryInt("page", default = 1)
            val limit = call.queryInt("limit", default = 20)
            call.respond(service.list(page, limit))
        }

        get("/{id}") {
            call.respond(service.get(call.userId()).toResponse())
        }

        patch("/{id}") {
            val user = service.update(call.userId(), call.receive<UpdateUserRequest>())
            call.respond(user.toResponse())
        }

        delete("/{id}") {
            service.disable(call.userId())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.authenticatedUserId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

private fun io.ktor.server.application.ApplicationCall.userId(): UUID {
    val rawId = parameters["id"]
    return runCatching { UUID.fromString(rawId) }
        .getOrElse {
            throw UserValidationException(mapOf("id" to "must be a valid UUID"))
        }
}

private fun io.ktor.server.application.ApplicationCall.queryInt(name: String, default: Int): Int {
    val rawValue = request.queryParameters[name] ?: return default
    return rawValue.toIntOrNull()
        ?: throw UserValidationException(mapOf(name to "must be an integer"))
}
