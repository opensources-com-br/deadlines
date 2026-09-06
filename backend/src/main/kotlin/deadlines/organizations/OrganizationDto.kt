package deadlines.organizations

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrganizationRequest(
    val name: String,
    val slug: String,
)

@Serializable
data class UpdateOrganizationRequest(
    val name: String? = null,
    val slug: String? = null,
)

@Serializable
data class OrganizationResponse(
    val id: String,
    val name: String,
    val slug: String,
    val role: String,
    val createdAt: String,
    val updatedAt: String,
)

fun OrganizationContext.toResponse() =
    OrganizationResponse(
        id = organization.id.toString(),
        name = organization.name,
        slug = organization.slug,
        role = membership.role.name.lowercase(),
        createdAt = organization.createdAt.toString(),
        updatedAt = organization.updatedAt.toString(),
    )
