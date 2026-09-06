package deadlines.organizations.invitations

import deadlines.organizations.access.Role
import java.time.Instant
import java.util.UUID

data class OrganizationInvitation(
    val id: UUID,
    val organizationId: UUID,
    val organizationName: String,
    val email: String,
    val role: Role,
    val invitedBy: UUID,
    val tokenHash: String,
    val status: InvitationStatus,
    val expiresAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val acceptedBy: UUID? = null,
    val acceptedAt: Instant? = null,
    val revokedAt: Instant? = null,
)

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    REVOKED,
    EXPIRED,
}

data class IssuedInvitation(
    val invitation: OrganizationInvitation,
    val rawToken: String,
)
