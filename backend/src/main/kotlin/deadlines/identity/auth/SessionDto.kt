package deadlines.identity.auth

import kotlinx.serialization.Serializable

@Serializable
data class SessionResponse(
    val id: String,
    val userAgent: String? = null,
    val ipAddress: String? = null,
    val expiresAt: String,
    val createdAt: String,
    val isCurrent: Boolean,
)

@Serializable
data class SessionListResponse(
    val data: List<SessionResponse>,
)
