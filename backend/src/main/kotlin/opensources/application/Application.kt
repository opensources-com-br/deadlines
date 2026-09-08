package opensources.application

import opensources.organizations.audits.AuditService
import opensources.organizations.audits.ExposedAuditRepository
import opensources.organizations.audits.auditRoutes

import opensources.config.AppConfig
import opensources.config.EmailProvider
import opensources.identity.auth.AuthOperations
import opensources.identity.auth.AuthenticationAbuseProtection
import opensources.identity.auth.AuthService
import opensources.identity.auth.BcryptPasswordHasher
import opensources.identity.auth.ExposedSessionRepository
import opensources.identity.auth.SessionService
import opensources.identity.auth.TokenService
import opensources.identity.email.EmailVerificationOperations
import opensources.identity.email.EmailVerificationService
import opensources.identity.email.ExposedEmailTokenRepository
import opensources.identity.email.LoggingEmailService
import opensources.identity.email.ResendEmailService
import opensources.identity.email.PasswordResetOperations
import opensources.identity.email.PasswordResetService
import opensources.identity.email.EmailChangeOperations
import opensources.identity.email.EmailChangeService
import opensources.identity.email.ExposedEmailChangeRepository
import opensources.identity.users.ExposedUserCredentialsRepository
import opensources.identity.users.ExposedUserRepository
import opensources.identity.users.AccountLifecycleService
import opensources.identity.users.AccountPasswordVerifier
import opensources.identity.users.ExposedAccountLifecycleRepository
import opensources.identity.users.UserService
import opensources.identity.users.UserRepository
import opensources.identity.users.ActiveAccountOperations
import opensources.identity.users.ActiveAccountService
import opensources.identity.preferences.ExposedUserPreferenceRepository
import opensources.identity.preferences.UserPreferenceOperations
import opensources.identity.preferences.UserPreferenceService
import opensources.organizations.ExposedOrganizationRepository
import opensources.organizations.OrganizationOperations
import opensources.organizations.OrganizationService
import opensources.organizations.access.ExposedPermissionRepository
import opensources.organizations.access.PermissionOperations
import opensources.organizations.access.PermissionService
import opensources.organizations.access.ExposedRoleRepository
import opensources.organizations.access.RoleOperations
import opensources.organizations.access.RoleService
import opensources.organizations.authorization.AuthorizationOperations
import opensources.organizations.authorization.AuthorizationService
import opensources.organizations.authorization.ExposedAuthorizationRepository
import opensources.organizations.invitations.ExposedInvitationRepository
import opensources.organizations.invitations.InvitationOperations
import opensources.organizations.invitations.InvitationService
import opensources.organizations.members.ExposedMemberRepository
import opensources.organizations.members.MemberOperations
import opensources.organizations.members.MemberService
import opensources.plans.PlanOperations
import opensources.subscriptions.SubscriptionOperations
import opensources.modules.billing.billingModule
import opensources.shared.database.DatabaseFactory
import opensources.shared.database.DatabaseQuery
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
        val userPreferenceService = UserPreferenceService(ExposedUserPreferenceRepository(query))
        val credentialsRepository = ExposedUserCredentialsRepository(query)
        val sessionRepository = ExposedSessionRepository(query)
        val sessionService = SessionService(sessionRepository)
        val organizationRepository = ExposedOrganizationRepository(query)
        val authorizationService = AuthorizationService(ExposedAuthorizationRepository(query))
        val billing = config.features.takeIf { it.billingEnabled }?.let { billingModule(query, authorizationService) }
        val auditService = AuditService(authorizationService, ExposedAuditRepository(query))
        val organizationService = OrganizationService(organizationRepository, authorizationService)
        val permissionRepository = ExposedPermissionRepository(query)
        val permissionService = PermissionService(authorizationService, permissionRepository)
        val roleRepository = ExposedRoleRepository(query)
        val roleService = RoleService(authorizationService, roleRepository, permissionRepository)
        val memberRepository = ExposedMemberRepository(query)
        val memberService = MemberService(authorizationService, memberRepository, roleRepository)
        val passwordHasher = BcryptPasswordHasher()
        val accountLifecycleService =
            AccountLifecycleService(
                userRepository,
                organizationRepository,
                AccountPasswordVerifier(credentialsRepository, passwordHasher),
                ExposedAccountLifecycleRepository(query),
            )
        val emailTokens = ExposedEmailTokenRepository(query)
        val emailService =
            when (config.email.provider) {
                EmailProvider.LOGGING -> LoggingEmailService()
                EmailProvider.RESEND -> ResendEmailService(config.email.resendApiKey!!, config.email.from)
            }
        val emailVerificationService = EmailVerificationService(userRepository, emailTokens, emailService, config.email)
        val emailChangeService = EmailChangeService(
            userRepository, AccountPasswordVerifier(credentialsRepository, passwordHasher),
            ExposedEmailChangeRepository(query), emailService, config.email,
        )
        val invitationService =
            InvitationService(
                organizationRepository,
                authorizationService,
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
        val abuseProtection = AuthenticationAbuseProtection(config.abuseProtection)
        val passwordResetService =
            PasswordResetService(credentialsRepository, emailTokens, emailService, passwordHasher, sessionRepository, config.email)

        embeddedServer(Netty, port = config.http.port) {
            module(
            userService,
                userRepository,
                authService,
                tokenService,
                emailVerificationService,
                passwordResetService,
                emailChangeService,
                sessionService,
                organizationService,
                permissionService,
                roleService,
                memberService,
                invitationService,
                auditService,
                billing?.plans,
                billing?.subscriptions,
                userPreferenceService,
                authorizationService,
                accountLifecycleService,
                abuseProtection,
                { database.isReady() },
            )
        }.start(wait = true)
    }
}

fun Application.module(
    userService: UserService? = null,
    userRepository: UserRepository? = null,
    authService: AuthOperations? = null,
    tokenService: TokenService? = null,
    emailVerification: EmailVerificationOperations? = null,
    passwordReset: PasswordResetOperations? = null,
    emailChangeService: EmailChangeOperations? = null,
    sessionService: SessionService? = null,
    organizationService: OrganizationOperations? = null,
    permissionService: PermissionOperations? = null,
    roleService: RoleOperations? = null,
    memberService: MemberOperations? = null,
    invitationService: InvitationOperations? = null,
    auditService: AuditService? = null,
    planService: PlanOperations? = null,
    subscriptionService: SubscriptionOperations? = null,
    userPreferenceService: UserPreferenceOperations? = null,
    authorizationService: AuthorizationOperations? = null,
    accountLifecycleService: opensources.identity.users.AccountLifecycleOperations? = null,
    abuseProtection: AuthenticationAbuseProtection = AuthenticationAbuseProtection(opensources.config.AbuseProtectionConfig()),
    readinessCheck: () -> Boolean = { true },
) {
    configurePlugins(tokenService, userRepository?.let(::ActiveAccountService))
    configureRoutes(
        userService,
        authService,
        emailVerification,
        passwordReset,
        emailChangeService,
        sessionService,
        organizationService,
        permissionService,
        roleService,
        memberService,
        invitationService,
        auditService,
        planService,
        subscriptionService,
        userPreferenceService,
        authorizationService,
        accountLifecycleService,
        abuseProtection,
        readinessCheck,
    )
}
