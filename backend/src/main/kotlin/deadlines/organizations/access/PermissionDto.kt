package deadlines.organizations.access

import kotlinx.serialization.Serializable

@Serializable
data class CreatePermissionRequest(
    val key: String,
    val name: String,
    val description: String? = null,
)

@Serializable
data class UpdatePermissionRequest(
    val key: String? = null,
    val name: String? = null,
    val description: String? = null,
)

@Serializable
data class PermissionResponse(
    val id: String,
    val key: String,
    val name: String,
    val description: String? = null,
    val isSystem: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class PermissionListResponse(
    val data: List<PermissionResponse>,
)

fun Permission.toResponse() =
    PermissionResponse(
        id = id.toString(),
        key = key,
        name = name,
        description = description,
        isSystem = isSystem,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
