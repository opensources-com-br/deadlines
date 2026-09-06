package deadlines.organizations.access

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.roleRoutes(service: RoleOperations) {
    authenticate("auth-jwt") {
        route("/api/v1/roles") {
            get {
                call.respond(service.list(call.accessUserId()))
            }
            post {
                val role = service.create(call.accessUserId(), call.receive())
                call.response.header(HttpHeaders.Location, "/api/v1/roles/${role.id}")
                call.respond(HttpStatusCode.Created, role)
            }
            route("/{roleId}") {
                get {
                    call.respond(service.get(call.accessUserId(), call.accessResourceId("roleId")))
                }
                patch {
                    call.respond(service.update(call.accessUserId(), call.accessResourceId("roleId"), call.receive()))
                }
                delete {
                    service.delete(call.accessUserId(), call.accessResourceId("roleId"))
                    call.respond(HttpStatusCode.NoContent)
                }
                route("/permissions") {
                    get {
                        call.respond(service.listPermissions(call.accessUserId(), call.accessResourceId("roleId")))
                    }
                    put {
                        call.respond(
                            service.replacePermissions(
                                call.accessUserId(),
                                call.accessResourceId("roleId"),
                                call.receive(),
                            ),
                        )
                    }
                }
            }
        }
    }
}
