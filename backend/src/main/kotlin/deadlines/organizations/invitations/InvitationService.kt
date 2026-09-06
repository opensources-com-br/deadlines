package deadlines.organizations.invitations

import deadlines.organizations.audits.withAuditActor

import deadlines.config.EmailConfig
import deadlines.identity.email.EmailMessage
import deadlines.identity.email.EmailService
import deadlines.identity.email.EmailTokenGenerator
import deadlines.identity.email.SecureEmailTokenGenerator
import deadlines.identity.users.UserRepository
import deadlines.identity.users.UserStatus
import deadlines.organizations.ActiveMembershipAlreadyExistsException
import deadlines.organizations.MembershipRole
import deadlines.organizations.OrganizationAccessDeniedException
import deadlines.organizations.OrganizationNotFoundException
import deadlines.organizations.OrganizationRepository
import deadlines.organizations.access.RoleNotFoundException
import deadlines.organizations.access.RoleRepository
import deadlines.organizations.members.MemberRepository
import deadlines.organizations.members.MemberResponse
import deadlines.organizations.members.toResponse
import java.time.Clock
import java.util.UUID

interface InvitationOperations {
    suspend fun list(userId: UUID): InvitationListResponse

    suspend fun get(userId: UUID, invitationId: UUID): InvitationResponse

    suspend fun create(userId: UUID, request: CreateInvitationRequest): InvitationResponse

    suspend fun resend(userId: UUID, invitationId: UUID): InvitationResponse

    suspend fun revoke(userId: UUID, invitationId: UUID)

    suspend fun preview(rawToken: String): InvitationPreviewResponse

    suspend fun accept(userId: UUID, rawToken: String): MemberResponse
}

class InvitationService(
    private val organizations: OrganizationRepository,
    private val invitations: InvitationRepository,
    private val roles: RoleRepository,
    private val members: MemberRepository,
    private val users: UserRepository,
    private val email: EmailService,
    private val config: EmailConfig,
    private val tokenGenerator: EmailTokenGenerator = SecureEmailTokenGenerator(),
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> UUID = UUID::randomUUID,
) : InvitationOperations {
    override suspend fun list(userId: UUID): InvitationListResponse {
        val context = currentOrganization(userId)
        return InvitationListResponse(invitations.list(context.organization.id, clock.instant()).map(OrganizationInvitation::toResponse))
    }

    override suspend fun get(userId: UUID, invitationId: UUID): InvitationResponse {
        val context = currentOrganization(userId)
        return requireInvitation(context.organization.id, invitationId).toResponse()
    }

    override suspend fun create(userId: UUID, request: CreateInvitationRequest): InvitationResponse = withAuditActor(userId) {
        val context = requireOwner(userId)
        val normalizedEmail = validateEmail(request.email)
        val role = requireAssignableRole(context.organization.id, request.roleId)
        val existingUser = users.findByEmail(normalizedEmail)
        if (existingUser != null && members.findByUserId(context.organization.id, existingUser.id) != null) {
            throw InvitationForMemberException()
        }

        val now = clock.instant()
        val rawToken = tokenGenerator.generate()
        val invitation =
            OrganizationInvitation(
                id = idGenerator(),
                organizationId = context.organization.id,
                organizationName = context.organization.name,
                email = normalizedEmail,
                role = role,
                invitedBy = userId,
                tokenHash = tokenGenerator.hash(rawToken),
                status = InvitationStatus.PENDING,
                expiresAt = now.plusSeconds(config.invitationExpirationSeconds),
                createdAt = now,
                updatedAt = now,
        )
        invitations.create(invitation)
        try {
            sendInvitation(invitation, rawToken)
        } catch (exception: Exception) {
            invitations.revoke(context.organization.id, invitation.id, clock.instant())
            throw exception
        }
        return@withAuditActor invitation.toResponse()
    }

    override suspend fun resend(userId: UUID, invitationId: UUID): InvitationResponse = withAuditActor(userId) {
        val context = requireOwner(userId)
        val invitation = requireInvitation(context.organization.id, invitationId)
        if (invitation.status !in setOf(InvitationStatus.PENDING, InvitationStatus.EXPIRED)) {
            throw InvitationInvalidException()
        }

        val now = clock.instant()
        val rawToken = tokenGenerator.generate()
        val renewed =
            invitation.copy(
                tokenHash = tokenGenerator.hash(rawToken),
                status = InvitationStatus.PENDING,
                expiresAt = now.plusSeconds(config.invitationExpirationSeconds),
                updatedAt = now,
            )
        if (!invitations.renew(context.organization.id, invitationId, renewed.tokenHash, renewed.expiresAt, now)) {
            throw InvitationNotFoundException()
        }
        sendInvitation(renewed, rawToken)
        return@withAuditActor renewed.toResponse()
    }

    override suspend fun revoke(userId: UUID, invitationId: UUID) = withAuditActor(userId) {
        val context = requireOwner(userId)
        requireInvitation(context.organization.id, invitationId)
        if (!invitations.revoke(context.organization.id, invitationId, clock.instant())) throw InvitationInvalidException()
    }

    override suspend fun preview(rawToken: String): InvitationPreviewResponse =
        findByRawToken(rawToken).toPreviewResponse()

    override suspend fun accept(userId: UUID, rawToken: String): MemberResponse = withAuditActor(userId) {
        val invitation = findByRawToken(rawToken)
        if (invitation.status != InvitationStatus.PENDING) throw InvitationInvalidException()
        val user = users.findById(userId)?.takeIf { it.status == UserStatus.ACTIVE } ?: throw InvitationInvalidException()
        if (!user.email.equals(invitation.email, ignoreCase = true)) throw InvitationEmailMismatchException()

        val current = organizations.findCurrentByUser(userId)
        if (current != null) {
            if (current.organization.id == invitation.organizationId) throw InvitationForMemberException()
            throw ActiveMembershipAlreadyExistsException()
        }

        if (!invitations.accept(invitation, userId, idGenerator(), clock.instant())) throw InvitationInvalidException()
        return@withAuditActor members.findByUserId(invitation.organizationId, userId)?.toResponse() ?: throw InvitationInvalidException()
    }

    private suspend fun sendInvitation(invitation: OrganizationInvitation, rawToken: String) {
        email.send(
            EmailMessage(
                to = invitation.email,
                subject = "You are invited to ${invitation.organizationName}",
                text =
                    "${invitation.organizationName} invited you to join as ${invitation.role.name}. " +
                        "Accept the invitation: ${config.appBaseUrl}/invitations/accept?token=$rawToken",
            ),
        )
    }

    private suspend fun findByRawToken(rawToken: String): OrganizationInvitation {
        if (rawToken.isBlank()) throw InvitationInvalidException()
        return invitations.findByTokenHash(tokenGenerator.hash(rawToken), clock.instant()) ?: throw InvitationInvalidException()
    }

    private suspend fun requireInvitation(organizationId: UUID, invitationId: UUID) =
        invitations.findById(organizationId, invitationId, clock.instant()) ?: throw InvitationNotFoundException()

    private suspend fun currentOrganization(userId: UUID) =
        organizations.findCurrentByUser(userId) ?: throw OrganizationNotFoundException()

    private suspend fun requireOwner(userId: UUID) =
        currentOrganization(userId).also {
            if (it.membership.role != MembershipRole.OWNER) throw OrganizationAccessDeniedException()
        }

    private suspend fun requireAssignableRole(organizationId: UUID, rawRoleId: String) =
        roles.findById(organizationId, rawRoleId.toUuid("roleId"))
            ?.takeUnless { it.key == "owner" }
            ?: throw RoleNotFoundException()

    private fun validateEmail(value: String): String {
        val normalized = value.trim().lowercase()
        if (!EMAIL_PATTERN.matches(normalized) || normalized.length > 320) {
            throw InvitationValidationException(mapOf("email" to "must be a valid email address"))
        }
        return normalized
    }

    private fun String.toUuid(field: String): UUID =
        runCatching { UUID.fromString(this) }.getOrElse {
            throw InvitationValidationException(mapOf(field to "must be a valid UUID"))
        }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
