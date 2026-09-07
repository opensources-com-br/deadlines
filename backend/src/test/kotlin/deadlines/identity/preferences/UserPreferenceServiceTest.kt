package deadlines.identity.preferences

import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserPreferenceServiceTest {
    private val now = Instant.parse("2026-09-07T12:00:00Z")
    private val userId = UUID.randomUUID()
    private val repository = MemoryUserPreferenceRepository(
        UserPreference(userId, "pt-BR", "America/Sao_Paulo", "system", now, now),
    )
    private val service = UserPreferenceService(repository, Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC))

    @Test
    fun `updates only the provided preferences`() = runTest {
        val response = service.update(userId, UpdateUserPreferenceRequest(locale = "en"))

        assertEquals("en", response.locale)
        assertEquals("America/Sao_Paulo", response.timezone)
        assertEquals("system", response.theme)
        assertEquals(now.plusSeconds(60).toString(), response.updatedAt)
    }

    @Test
    fun `accepts valid IANA timezone and supported theme`() = runTest {
        val response = service.update(userId, UpdateUserPreferenceRequest(timezone = "Europe/Lisbon", theme = "dark"))

        assertEquals("Europe/Lisbon", response.timezone)
        assertEquals("dark", response.theme)
    }

    @Test
    fun `rejects unsupported preference values`() = runTest {
        assertFailsWith<UserPreferenceValidationException> {
            service.update(userId, UpdateUserPreferenceRequest(locale = "es", timezone = "Invalid/Zone", theme = "blue"))
        }
    }

    @Test
    fun `rejects an empty update`() = runTest {
        assertFailsWith<UserPreferenceValidationException> { service.update(userId, UpdateUserPreferenceRequest()) }
    }
}

private class MemoryUserPreferenceRepository(initial: UserPreference) : UserPreferenceRepository {
    private var value = initial
    override suspend fun findByUserId(userId: UUID) = value.takeIf { it.userId == userId }
    override suspend fun update(userId: UUID, locale: String, timezone: String, theme: String, updatedAt: Instant): UserPreference? {
        if (value.userId != userId) return null
        value = value.copy(locale = locale, timezone = timezone, theme = theme, updatedAt = updatedAt)
        return value
    }
}
