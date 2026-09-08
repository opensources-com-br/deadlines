package opensources.identity.preferences

import kotlinx.serialization.Serializable
import opensources.shared.errors.ApiException
import java.time.Instant
import java.util.UUID

data class UserPreference(
    val userId: UUID,
    val locale: String,
    val timezone: String,
    val theme: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class UserPreferenceResponse(
    val locale: String,
    val timezone: String,
    val theme: String,
    val updatedAt: String,
)

@Serializable
data class UpdateUserPreferenceRequest(
    val locale: String? = null,
    val timezone: String? = null,
    val theme: String? = null,
)

fun UserPreference.toResponse() = UserPreferenceResponse(locale, timezone, theme, updatedAt.toString())

class UserPreferenceValidationException(violations: Map<String, String>) :
    ApiException(422, "VALIDATION_ERROR", "Invalid user preferences", violations)
class UserPreferenceNotFoundException : ApiException(404, "USER_PREFERENCES_NOT_FOUND", "User preferences were not found")
