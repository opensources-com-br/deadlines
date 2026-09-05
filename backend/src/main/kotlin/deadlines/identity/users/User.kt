package deadlines.identity.users

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val status: UserStatus,
    val profile: UserProfile,
    val createdAt: Instant,
    val updatedAt: Instant,
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
}
