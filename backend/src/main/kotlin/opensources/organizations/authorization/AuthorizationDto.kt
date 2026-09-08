package opensources.organizations.authorization

import kotlinx.serialization.Serializable

@Serializable
data class AuthorizationContextResponse(
    val organizationId: String,
    val membershipId: String,
    val roleId: String,
    val permissions: List<String>,
)

fun AuthorizationContext.toResponse() =
    AuthorizationContextResponse(
        organizationId = organizationId.toString(),
        membershipId = membershipId.toString(),
        roleId = roleId.toString(),
        permissions = permissions.sorted(),
    )
