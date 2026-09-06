package deadlines.application

import deadlines.organizations.audits.AuditService
import deadlines.organizations.audits.ExposedAuditRepository
import deadlines.organizations.audits.auditRoutes

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
import deadlines.organizations.access.ExposedPermissionRepository
import deadlines.organizations.access.PermissionOperations
import deadlines.organizations.access.PermissionService
import deadlines.organizations.access.ExposedRoleRepository
import deadlines.organizations.access.RoleOperations
import deadlines.organizations.access.RoleService
import deadlines.organizations.invitations.ExposedInvitationRepository
import deadlines.organizations.invitations.InvitationOperations
import deadlines.organizations.invitations.InvitationService
import deadlines.organizations.members.ExposedMemberRepository
import deadlines.organizations.members.MemberOperations
import deadlines.organizations.members.MemberService
import deadlines.plans.ExposedPlanRepository
import deadlines.plans.PlanOperations
import deadlines.plans.PlanService
import deadlines.subscriptions.ExposedSubscriptionRepository
import deadlines.subscriptions.SubscriptionOperations
import deadlines.subscriptions.SubscriptionService
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
        val organizationRepository = ExposedOrganizationRepository(query)
        val planService = PlanService(ExposedPlanRepository(query))
        val subscriptionService = SubscriptionService(organizationRepository, ExposedSubscriptionRepository(query))
        val auditService = AuditService(organizationRepository, ExposedAuditRepository(query))
        val organizationService = OrganizationService(organizationRepository)
        val permissionRepository = ExposedPermissionRepository(query)
        val permissionService = PermissionService(organizationRepository, permissionRepository)
        val roleRepository = ExposedRoleRepository(query)
        val roleService = RoleService(organizationRepository, roleRepository, permissionRepository)
        val memberRepository = ExposedMemberRepository(query)
        val memberService = MemberService(organizationRepository, memberRepository, roleRepository)
        val passwordHasher = BcryptPasswordHasher()
        val emailTokens = ExposedEmailTokenRepository(query)
        val emailService =
            when (config.email.provider) {
                EmailProvider.LOGGING -> LoggingEmailService()
                EmailProvider.RESEND -> ResendEmailService(config.email.resendApiKey!!, config.email.from)
            }
        val emailVerificationService = EmailVerificationService(userRepository, emailTokens, emailService, config.email)
        val invitationService =
            InvitationService(
                organizationRepository,
                ExposedInvitationRepository(query),
                roleRepository,
                memberRepository,
                userRepository,
                emailService,
                config.email,
            )
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
                permissionService,
                roleService,
                memberService,
                invitationService,
                auditService,
                planService,
                subscriptionService,
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
    permissionService: PermissionOperations? = null,
    roleService: RoleOperations? = null,
    memberService: MemberOperations? = null,
    invitationService: InvitationOperations? = null,
    auditService: AuditService? = null,
    planService: PlanOperations? = null,
    subscriptionService: SubscriptionOperations? = null,
) {
    configurePlugins(tokenService)
    configureRoutes(
        userService,
        authService,
        emailVerification,
        passwordReset,
        sessionService,
        organizationService,
        permissionService,
        roleService,
        memberService,
        invitationService,
        auditService,
        planService,
        subscriptionService,
    )
}
