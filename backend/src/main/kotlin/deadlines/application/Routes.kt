package deadlines.application

import deadlines.identity.auth.AuthOperations
import deadlines.identity.auth.authRoutes
import deadlines.identity.email.EmailVerificationOperations
import deadlines.identity.email.PasswordResetOperations
import deadlines.identity.email.emailRoutes
import deadlines.identity.users.UserService
import deadlines.identity.users.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
)

fun Application.configureRoutes(
    userService: UserService?,
    authService: AuthOperations?,
    emailVerification: EmailVerificationOperations? = null,
    passwordReset: PasswordResetOperations? = null,
) {
    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }

        if (userService != null) {
            userRoutes(userService)
        }
        if (authService != null) {
            authRoutes(authService)
        }
        if (emailVerification != null && passwordReset != null) {
            emailRoutes(emailVerification, passwordReset)
        }
    }
}
