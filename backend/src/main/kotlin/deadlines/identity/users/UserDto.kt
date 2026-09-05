package deadlines.identity.users

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val phone: String? = null,
)

@Serializable
data class UpdateUserRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val status: String? = null,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val status: String,
    val profile: UserProfileResponse,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class UserProfileResponse(
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val phone: String? = null,
)

@Serializable
data class UserListResponse(
    val data: List<UserResponse>,
    val pagination: PaginationResponse,
)

@Serializable
data class PaginationResponse(
    val page: Int,
    val limit: Int,
    val total: Long,
    val totalPages: Int,
)

fun User.toResponse() =
    UserResponse(
        id = id.toString(),
        email = email,
        status = status.name.lowercase(),
        profile =
            UserProfileResponse(
                firstName = profile.firstName,
                lastName = profile.lastName,
                avatarUrl = profile.avatarUrl,
                phone = profile.phone,
            ),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
