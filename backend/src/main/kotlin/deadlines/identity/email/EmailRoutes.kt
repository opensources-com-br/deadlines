package deadlines.identity.email

import deadlines.identity.auth.AuthenticationAbuseProtection
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
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
    abuseProtection: AuthenticationAbuseProtection,
) {
    route("/api/v1/auth") {
        post("/email/verify") {
            abuseProtection.checkRateLimit("verify-email", call.request.origin.remoteHost)
            verification.verify(call.receive<VerifyEmailRequest>().token)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            abuseProtection.checkRateLimit("forgot-password", call.request.origin.remoteHost, request.email)
            passwordReset.request(request.email)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            abuseProtection.checkRateLimit("reset-password", call.request.origin.remoteHost)
            passwordReset.reset(request.token, request.password)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/email/resend") {
            val request = call.receive<ResendVerificationRequest>()
            abuseProtection.checkRateLimit("resend-verification", call.request.origin.remoteHost, request.email)
            verification.resendForEmail(request.email)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
