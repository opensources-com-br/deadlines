package deadlines.identity.email

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class VerifyEmailRequest(
    val token: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ResendVerificationRequest(
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
        post("/email/resend") {
            verification.resendForEmail(call.receive<ResendVerificationRequest>().email)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
