package opensources.identity.email

import opensources.identity.auth.AuthenticationAbuseProtection
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import opensources.application.clientAddress
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
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

@Serializable
data class RequestEmailChangeRequest(val email: String, val password: String)

@Serializable
data class ConfirmEmailChangeRequest(val token: String)

fun Route.emailRoutes(
    verification: EmailVerificationOperations,
    passwordReset: PasswordResetOperations,
    abuseProtection: AuthenticationAbuseProtection,
    emailChanges: EmailChangeOperations? = null,
) {
    route("/api/v1/auth") {
        post("/email/verify") {
            abuseProtection.checkRateLimit("verify-email", call.clientAddress())
            verification.verify(call.receive<VerifyEmailRequest>().token)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            abuseProtection.checkRateLimit("forgot-password", call.clientAddress(), request.email)
            passwordReset.request(request.email)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            abuseProtection.checkRateLimit("reset-password", call.clientAddress())
            passwordReset.reset(request.token, request.password)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/email/resend") {
            val request = call.receive<ResendVerificationRequest>()
            abuseProtection.checkRateLimit("resend-verification", call.clientAddress(), request.email)
            verification.resendForEmail(request.email)
            call.respond(HttpStatusCode.NoContent)
        }
        if (emailChanges != null) {
            post("/email/change/confirm") {
                emailChanges.confirm(call.receive<ConfirmEmailChangeRequest>().token)
                call.respond(HttpStatusCode.NoContent)
            }
            authenticate("auth-jwt") {
                post("/email/change") {
                    val request = call.receive<RequestEmailChangeRequest>()
                    val userId = java.util.UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
                    emailChanges.request(userId, request.email, request.password)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
