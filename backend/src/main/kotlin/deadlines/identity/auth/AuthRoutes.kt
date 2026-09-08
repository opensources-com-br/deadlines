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
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.authRoutes(auth: AuthOperations, abuseProtection: AuthenticationAbuseProtection) {
    route("/api/v1/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            abuseProtection.checkRateLimit("register", call.sessionContext().ipAddress, request.email)
            call.respond(HttpStatusCode.Created, auth.register(request, call.sessionContext()))
        }
        post("/login") {
            val request = call.receive<LoginRequest>()
            val context = call.sessionContext()
            abuseProtection.checkRateLimit("login", context.ipAddress, request.email)
            abuseProtection.checkLoginLockout(request.email, context.ipAddress)
            try {
                call.respond(auth.login(request, context))
                abuseProtection.recordLogin(request.email, context.ipAddress, successful = true)
            } catch (error: InvalidCredentialsException) {
                abuseProtection.recordLogin(request.email, context.ipAddress, successful = false)
                throw error
            }
        }
        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            abuseProtection.checkRateLimit("refresh", call.sessionContext().ipAddress)
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
            patch("/password") {
                val subject = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(auth.changePassword(UUID.fromString(subject), call.receive(), call.sessionContext()))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.sessionContext() =
    SessionContext(
        userAgent = request.headers[HttpHeaders.UserAgent],
        ipAddress = request.origin.remoteHost,
        deviceId = request.headers[DEVICE_ID_HEADER]?.let { value ->
            runCatching { UUID.fromString(value) }.getOrElse {
                throw AuthValidationException(mapOf(DEVICE_ID_HEADER to "must be a UUID"))
            }
        },
    )

private const val DEVICE_ID_HEADER = "X-Device-Id"
