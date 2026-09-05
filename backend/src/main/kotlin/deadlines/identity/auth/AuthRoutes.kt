package deadlines.identity.auth

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.authRoutes(auth: AuthOperations) {
    route("/api/v1/auth") {
        post("/register") {
            call.respond(HttpStatusCode.Created, auth.register(call.receive(), call.sessionContext()))
        }
        post("/login") {
            call.respond(auth.login(call.receive(), call.sessionContext()))
        }
        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            call.respond(auth.refresh(request.refreshToken, call.sessionContext()))
        }
        post("/logout") {
            auth.logout(call.receive<RefreshTokenRequest>().refreshToken)
            call.respond(HttpStatusCode.NoContent)
        }
        authenticate("auth-jwt") {
            get("/me") {
                val subject = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(auth.me(UUID.fromString(subject)))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.sessionContext() =
    SessionContext(
        userAgent = request.headers[HttpHeaders.UserAgent],
        ipAddress = request.origin.remoteHost,
    )
