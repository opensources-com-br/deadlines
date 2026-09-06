package deadlines.identity.email

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class VerifyEmailRequest(
    val token: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val password: String,
)

fun Route.emailRoutes(
    verification: EmailVerificationOperations,
    passwordReset: PasswordResetOperations,
) {
    route("/api/v1/auth") {
        post("/email/verify") {
            verification.verify(call.receive<VerifyEmailRequest>().token)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/forgot-password") {
            passwordReset.request(call.receive<ForgotPasswordRequest>().email)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            passwordReset.reset(request.token, request.password)
            call.respond(HttpStatusCode.NoContent)
        }
        authenticate("auth-jwt") {
            post("/email/resend") {
                val subject = call.principal<JWTPrincipal>()!!.payload.subject
                verification.resend(UUID.fromString(subject))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
