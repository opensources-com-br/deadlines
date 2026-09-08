package opensources.identity.preferences

import opensources.application.module
import opensources.config.AuthConfig
import opensources.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UserPreferenceRoutesTest {
    private val tokens = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `preferences require authentication`() = testApplication {
        application { module(tokenService = tokens, userPreferenceService = FakeUserPreferenceOperations()) }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/users/me/preferences").status)
    }

    @Test
    fun `authenticated user reads and updates own preferences`() = testApplication {
        val operations = FakeUserPreferenceOperations()
        val userId = UUID.randomUUID()
        application { module(tokenService = tokens, userPreferenceService = operations) }
        val accessToken = tokens.issue(userId).accessToken

        val getResponse = client.get("/api/v1/users/me/preferences") { bearerAuth(accessToken) }
        val patchResponse = client.patch("/api/v1/users/me/preferences") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"locale":"en","timezone":"Europe/Lisbon","theme":"dark"}""")
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertEquals(HttpStatusCode.OK, patchResponse.status)
        assertEquals(userId, operations.lastUserId)
        assertEquals("en", operations.lastRequest?.locale)
    }
}

private class FakeUserPreferenceOperations : UserPreferenceOperations {
    var lastUserId: UUID? = null
    var lastRequest: UpdateUserPreferenceRequest? = null
    private val response = UserPreferenceResponse("pt-BR", "America/Sao_Paulo", "system", "2026-09-07T12:00:00Z")
    override suspend fun get(userId: UUID): UserPreferenceResponse {
        lastUserId = userId
        return response
    }
    override suspend fun update(userId: UUID, request: UpdateUserPreferenceRequest): UserPreferenceResponse {
        lastUserId = userId
        lastRequest = request
        return response.copy(locale = request.locale ?: response.locale, timezone = request.timezone ?: response.timezone, theme = request.theme ?: response.theme)
    }
}
