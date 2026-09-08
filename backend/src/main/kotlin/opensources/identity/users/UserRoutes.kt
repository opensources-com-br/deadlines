package opensources.identity.users

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

fun Route.userRoutes(service: UserService, lifecycle: AccountLifecycleOperations? = null) {
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

                if (lifecycle != null) {
                    post("/deactivate") {
                        val request = call.receive<ConfirmAccountActionRequest>()
                        lifecycle.deactivate(call.authenticatedUserId(), request.password)
                        call.respond(HttpStatusCode.NoContent)
                    }

                    delete {
                        val request = call.receive<ConfirmAccountActionRequest>()
                        lifecycle.delete(call.authenticatedUserId(), request.password)
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }

    }
}

private fun io.ktor.server.application.ApplicationCall.authenticatedUserId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)
