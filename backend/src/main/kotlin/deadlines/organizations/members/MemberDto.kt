package deadlines.organizations.members

import deadlines.organizations.access.RoleResponse
import deadlines.organizations.access.toResponse
import kotlinx.serialization.Serializable

@Serializable
data class UpdateMemberRoleRequest(
    val roleId: String,
)

@Serializable
data class MemberResponse(
    val id: String,
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: RoleResponse,
    val joinedAt: String,
)

@Serializable
data class MemberListResponse(
    val data: List<MemberResponse>,
)

fun OrganizationMember.toResponse() =
    MemberResponse(
        id = membershipId.toString(),
        userId = userId.toString(),
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = role.toResponse(),
        joinedAt = joinedAt.toString(),
    )
