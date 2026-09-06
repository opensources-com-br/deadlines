package deadlines.organizations.invitations

import deadlines.organizations.access.RoleResponse
import deadlines.organizations.access.toResponse
import kotlinx.serialization.Serializable

@Serializable
data class CreateInvitationRequest(
    val email: String,
    val roleId: String,
)

@Serializable
data class AcceptInvitationRequest(
    val token: String,
)

@Serializable
data class InvitationResponse(
    val id: String,
    val organizationId: String,
    val organizationName: String,
    val email: String,
    val role: RoleResponse,
    val status: String,
    val expiresAt: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class InvitationListResponse(
    val data: List<InvitationResponse>,
)

@Serializable
data class InvitationPreviewResponse(
    val organizationName: String,
    val email: String,
    val roleName: String,
    val status: String,
    val expiresAt: String,
)

fun OrganizationInvitation.toResponse() =
    InvitationResponse(
        id = id.toString(),
        organizationId = organizationId.toString(),
        organizationName = organizationName,
        email = email,
        role = role.toResponse(),
        status = status.name.lowercase(),
        expiresAt = expiresAt.toString(),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

fun OrganizationInvitation.toPreviewResponse() =
    InvitationPreviewResponse(
        organizationName = organizationName,
        email = email,
        roleName = role.name,
        status = status.name.lowercase(),
        expiresAt = expiresAt.toString(),
    )
