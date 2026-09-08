package opensources.application

import opensources.organizations.audits.AuditService
import opensources.organizations.audits.ExposedAuditRepository
import opensources.organizations.audits.auditRoutes

import opensources.identity.auth.AuthOperations
import opensources.identity.auth.AuthenticationAbuseProtection
import opensources.identity.auth.authRoutes
import opensources.identity.auth.SessionService
import opensources.identity.auth.sessionRoutes
import opensources.identity.email.EmailVerificationOperations
import opensources.identity.email.PasswordResetOperations
import opensources.identity.email.EmailChangeOperations
import opensources.identity.email.emailRoutes
import opensources.identity.users.UserService
import opensources.identity.users.AccountLifecycleOperations
import opensources.identity.users.userRoutes
import opensources.identity.preferences.UserPreferenceOperations
import opensources.identity.preferences.userPreferenceRoutes
import opensources.organizations.OrganizationOperations
import opensources.organizations.organizationRoutes
import opensources.organizations.access.PermissionOperations
import opensources.organizations.access.permissionRoutes
import opensources.organizations.access.RoleOperations
import opensources.organizations.access.roleRoutes
import opensources.organizations.authorization.AuthorizationOperations
import opensources.organizations.authorization.authorizationRoutes
import opensources.organizations.invitations.InvitationOperations
import opensources.organizations.invitations.invitationRoutes
import opensources.organizations.members.MemberOperations
import opensources.organizations.members.memberRoutes
import opensources.modules.billing.BillingModule
import opensources.modules.billing.billingRoutes
import opensources.shared.errors.ApiException
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
    emailChangeService: EmailChangeOperations? = null,
    sessionService: SessionService? = null,
    organizationService: OrganizationOperations? = null,
    permissionService: PermissionOperations? = null,
    roleService: RoleOperations? = null,
    memberService: MemberOperations? = null,
    invitationService: InvitationOperations? = null,
    auditService: AuditService? = null,
    billing: BillingModule? = null,
    userPreferenceService: UserPreferenceOperations? = null,
    authorizationService: AuthorizationOperations? = null,
    accountLifecycleService: AccountLifecycleOperations? = null,
    abuseProtection: AuthenticationAbuseProtection,
    readinessCheck: () -> Boolean,
) {
    routing {
        if (billing != null) billingRoutes(billing)
        if (userPreferenceService != null) userPreferenceRoutes(userPreferenceService)
        if (authorizationService != null) authorizationRoutes(authorizationService)
        if (auditService != null) auditRoutes(auditService)
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }
        get("/health/live") {
            call.respond(HealthResponse(status = "ok"))
        }
        get("/health/ready") {
            if (!readinessCheck()) {
                throw ApiException(503, "NOT_READY", "Required dependencies are unavailable")
            }
            call.respond(HealthResponse(status = "ok"))
        }

        if (userService != null) {
            userRoutes(userService, accountLifecycleService)
        }
        if (authService != null) {
            authRoutes(authService, abuseProtection)
        }
        if (emailVerification != null && passwordReset != null) {
            emailRoutes(emailVerification, passwordReset, abuseProtection, emailChangeService)
        }
        if (sessionService != null) {
            sessionRoutes(sessionService)
        }
        if (organizationService != null) {
            organizationRoutes(organizationService)
        }
        if (permissionService != null) {
            permissionRoutes(permissionService)
        }
        if (roleService != null) {
            roleRoutes(roleService)
        }
        if (memberService != null) {
            memberRoutes(memberService)
        }
        if (invitationService != null) {
            invitationRoutes(invitationService, abuseProtection)
        }
    }
}
