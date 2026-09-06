package deadlines.organizations

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.organizationRoutes(service: OrganizationOperations) {
    authenticate("auth-jwt") {
        route("/api/v1/organizations") {
            post {
                val organization = service.create(call.authenticatedUserId(), call.receive())
                call.response.header(HttpHeaders.Location, "/api/v1/organizations/current")
                call.respond(HttpStatusCode.Created, organization)
            }

            route("/current") {
                get {
                    call.respond(service.current(call.authenticatedUserId()))
                }

                patch {
                    call.respond(service.update(call.authenticatedUserId(), call.receive()))
                }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.authenticatedUserId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)
