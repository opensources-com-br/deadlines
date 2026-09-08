package opensources.identity.preferences

import java.time.Clock
import java.time.DateTimeException
import java.time.ZoneId
import java.util.UUID

interface UserPreferenceOperations {
    suspend fun get(userId: UUID): UserPreferenceResponse
    suspend fun update(userId: UUID, request: UpdateUserPreferenceRequest): UserPreferenceResponse
}

class UserPreferenceService(
    private val repository: UserPreferenceRepository,
    private val clock: Clock = Clock.systemUTC(),
) : UserPreferenceOperations {
    override suspend fun get(userId: UUID): UserPreferenceResponse =
        repository.findByUserId(userId)?.toResponse() ?: throw UserPreferenceNotFoundException()

    override suspend fun update(userId: UUID, request: UpdateUserPreferenceRequest): UserPreferenceResponse {
        val current = repository.findByUserId(userId) ?: throw UserPreferenceNotFoundException()
        val locale = request.locale ?: current.locale
        val timezone = request.timezone ?: current.timezone
        val theme = request.theme ?: current.theme
        val violations = buildMap {
            if (request.locale == null && request.timezone == null && request.theme == null) {
                put("request", "must contain at least one field")
            }
            if (locale !in supportedLocales) put("locale", "must be one of: ${supportedLocales.joinToString()}")
            if (theme !in supportedThemes) put("theme", "must be one of: ${supportedThemes.joinToString()}")
            try {
                ZoneId.of(timezone)
            } catch (_: DateTimeException) {
                put("timezone", "must be a valid IANA timezone")
            }
        }
        if (violations.isNotEmpty()) throw UserPreferenceValidationException(violations)
        return repository.update(userId, locale, timezone, theme, clock.instant())?.toResponse()
            ?: throw UserPreferenceNotFoundException()
    }

    companion object {
        val supportedLocales = setOf("pt-BR", "en")
        val supportedThemes = setOf("light", "dark", "system")
    }
}
