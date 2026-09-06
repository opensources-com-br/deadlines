package deadlines.organizations.access

import kotlinx.serialization.Serializable

@Serializable
data class CreateRoleRequest(
    val key: String,
    val name: String,
    val description: String? = null,
)

@Serializable
data class UpdateRoleRequest(
    val key: String? = null,
    val name: String? = null,
    val description: String? = null,
)

@Serializable
data class ReplaceRolePermissionsRequest(
    val permissionIds: List<String>,
)

@Serializable
data class RoleResponse(
    val id: String,
    val key: String,
    val name: String,
    val description: String? = null,
    val isSystem: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class RoleListResponse(
    val data: List<RoleResponse>,
)

fun Role.toResponse() =
    RoleResponse(
        id = id.toString(),
        key = key,
        name = name,
        description = description,
        isSystem = isSystem,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
