package deadlines.application

import deadlines.organizations.audits.AuditService
import deadlines.organizations.audits.ExposedAuditRepository
import deadlines.organizations.audits.auditRoutes

import deadlines.identity.auth.AuthOperations
import deadlines.identity.auth.authRoutes
import deadlines.identity.auth.SessionService
import deadlines.identity.auth.sessionRoutes
import deadlines.identity.email.EmailVerificationOperations
import deadlines.identity.email.PasswordResetOperations
import deadlines.identity.email.emailRoutes
import deadlines.identity.users.UserService
import deadlines.identity.users.userRoutes
import deadlines.organizations.OrganizationOperations
import deadlines.organizations.organizationRoutes
import deadlines.organizations.access.PermissionOperations
import deadlines.organizations.access.permissionRoutes
import deadlines.organizations.access.RoleOperations
import deadlines.organizations.access.roleRoutes
import deadlines.organizations.invitations.InvitationOperations
import deadlines.organizations.invitations.invitationRoutes
import deadlines.organizations.members.MemberOperations
import deadlines.organizations.members.memberRoutes
import deadlines.plans.PlanOperations
import deadlines.plans.planRoutes
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
    sessionService: SessionService? = null,
    organizationService: OrganizationOperations? = null,
    permissionService: PermissionOperations? = null,
    roleService: RoleOperations? = null,
    memberService: MemberOperations? = null,
    invitationService: InvitationOperations? = null,
    auditService: AuditService? = null,
    planService: PlanOperations? = null,
) {
    routing {
        if (planService != null) planRoutes(planService)
        if (auditService != null) auditRoutes(auditService)
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
            invitationRoutes(invitationService)
        }
    }
}
