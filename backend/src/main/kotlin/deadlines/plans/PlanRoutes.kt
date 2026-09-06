package deadlines.plans

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.planRoutes(service: PlanOperations) {
    get("/api/v1/plans") { call.respond(service.list()) }
}
