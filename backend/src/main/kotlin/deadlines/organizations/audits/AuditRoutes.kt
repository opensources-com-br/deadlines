package deadlines.organizations.audits

import deadlines.organizations.access.accessUserId
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.auditRoutes(service: AuditService) {
    authenticate("auth-jwt") {
        get("/api/v1/audits") {
            val parameters = call.request.queryParameters.entries().associate { (key, values) ->
                if (values.size != 1) throw AuditValidationException(key)
                key to values.single()
            }
            call.respond(service.list(call.accessUserId(), parameters))
        }
    }
}
