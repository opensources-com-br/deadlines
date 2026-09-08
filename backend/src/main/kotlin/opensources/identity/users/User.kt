package opensources.identity.users

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val status: UserStatus,
    val profile: UserProfile,
    val createdAt: Instant,
    val updatedAt: Instant,
    val emailVerifiedAt: Instant? = null,
    val disabledAt: Instant? = null,
    val deletedAt: Instant? = null,
)

data class UserProfile(
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val phone: String?,
)

enum class UserStatus {
    PENDING,
    ACTIVE,
    DISABLED,
    DELETED,
}
