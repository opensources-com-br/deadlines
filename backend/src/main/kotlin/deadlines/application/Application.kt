package deadlines.application

import deadlines.config.AppConfig
import deadlines.config.EmailProvider
import deadlines.identity.auth.AuthOperations
import deadlines.identity.auth.AuthService
import deadlines.identity.auth.BcryptPasswordHasher
import deadlines.identity.auth.ExposedSessionRepository
import deadlines.identity.auth.SessionService
import deadlines.identity.auth.TokenService
import deadlines.identity.email.EmailVerificationOperations
import deadlines.identity.email.EmailVerificationService
import deadlines.identity.email.ExposedEmailTokenRepository
import deadlines.identity.email.LoggingEmailService
import deadlines.identity.email.ResendEmailService
import deadlines.identity.email.PasswordResetOperations
import deadlines.identity.email.PasswordResetService
import deadlines.identity.users.ExposedUserCredentialsRepository
import deadlines.identity.users.ExposedUserRepository
import deadlines.identity.users.UserService
import deadlines.organizations.ExposedOrganizationRepository
import deadlines.organizations.OrganizationOperations
import deadlines.organizations.OrganizationService
import deadlines.shared.database.DatabaseFactory
import deadlines.shared.database.DatabaseQuery
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = AppConfig.fromEnvironment()

    DatabaseFactory.open(config.database).use { database ->
        val query = DatabaseQuery(database.database)
        val userRepository = ExposedUserRepository(query)
        val tokenService = TokenService(config.auth)
        val userService = UserService(userRepository)
        val credentialsRepository = ExposedUserCredentialsRepository(query)
        val sessionRepository = ExposedSessionRepository(query)
        val sessionService = SessionService(sessionRepository)
        val organizationService = OrganizationService(ExposedOrganizationRepository(query))
        val passwordHasher = BcryptPasswordHasher()
        val emailTokens = ExposedEmailTokenRepository(query)
        val emailService =
            when (config.email.provider) {
                EmailProvider.LOGGING -> LoggingEmailService()
                EmailProvider.RESEND -> ResendEmailService(config.email.resendApiKey!!, config.email.from)
            }
        val emailVerificationService = EmailVerificationService(userRepository, emailTokens, emailService, config.email)
        val authService =
            AuthService(
                credentialsRepository,
                userRepository,
                sessionRepository,
                passwordHasher,
                tokenService,
                emailVerificationService,
            )
        val passwordResetService =
            PasswordResetService(credentialsRepository, emailTokens, emailService, passwordHasher, sessionRepository, config.email)

        embeddedServer(Netty, port = config.http.port) {
            module(
                userService,
                authService,
                tokenService,
                emailVerificationService,
                passwordResetService,
                sessionService,
                organizationService,
            )
        }.start(wait = true)
    }
}

fun Application.module(
    userService: UserService? = null,
    authService: AuthOperations? = null,
    tokenService: TokenService? = null,
    emailVerification: EmailVerificationOperations? = null,
    passwordReset: PasswordResetOperations? = null,
    sessionService: SessionService? = null,
    organizationService: OrganizationOperations? = null,
) {
    configurePlugins(tokenService)
    configureRoutes(userService, authService, emailVerification, passwordReset, sessionService, organizationService)
}
