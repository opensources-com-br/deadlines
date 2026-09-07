package deadlines.organizations.authorization

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthorizationRoutesTest {
    private val tokens = TokenService(
        AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600),
    )

    @Test
    fun `authorization context requires authentication`() = testApplication {
        application { module(tokenService = tokens, authorizationService = testAuthorization(UUID.randomUUID(), UUID.randomUUID())) }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/users/me/authorization").status)
    }

    @Test
    fun `authenticated user receives own identifiers and permissions`() = testApplication {
        val userId = UUID.randomUUID()
        val organizationId = UUID.randomUUID()
        val service = testAuthorization(userId, organizationId, "members.invite", "members.read")
        application { module(tokenService = tokens, authorizationService = service) }

        val response = client.get("/api/v1/users/me/authorization") {
            bearerAuth(tokens.issue(userId).accessToken)
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(organizationId.toString(), body.getValue("organizationId").jsonPrimitive.content)
        assertEquals(
            listOf("members.invite", "members.read"),
            body.getValue("permissions").jsonArray.map { it.jsonPrimitive.content },
        )
    }
}
