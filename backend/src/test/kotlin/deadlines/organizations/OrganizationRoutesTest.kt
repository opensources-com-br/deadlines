package deadlines.organizations

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationRoutesTest {
    private val tokenService =
        TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `organization routes require authentication`() =
        testApplication {
            application { module(tokenService = tokenService, organizationService = FakeOrganizationOperations()) }

            assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/organizations").status)
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/organizations/current").status)
            assertEquals(HttpStatusCode.Unauthorized, client.patch("/api/v1/organizations/current").status)
        }

    @Test
    fun `authenticated user can create read and update the current organization`() =
        testApplication {
            val userId = UUID.randomUUID()
            val service = FakeOrganizationOperations()
            val token = tokenService.issue(userId).accessToken
            application { module(tokenService = tokenService, organizationService = service) }

            val created =
                client.post("/api/v1/organizations") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Acme","slug":"acme"}""")
                }
            assertEquals(HttpStatusCode.Created, created.status)
            assertEquals("/api/v1/organizations/current", created.headers[HttpHeaders.Location])
            assertEquals(userId, service.lastUserId)
            assertEquals("Acme", service.createRequest?.name)

            val current = client.get("/api/v1/organizations/current") { bearerAuth(token) }
            assertEquals(HttpStatusCode.OK, current.status)
            assertEquals("owner", current.organizationRole())

            val updated =
                client.patch("/api/v1/organizations/current") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Updated"}""")
                }
            assertEquals(HttpStatusCode.OK, updated.status)
            assertEquals("Updated", service.updateRequest?.name)
        }

    @Test
    fun `rejects malformed organization json`() =
        testApplication {
            val token = tokenService.issue(UUID.randomUUID()).accessToken
            application { module(tokenService = tokenService, organizationService = FakeOrganizationOperations()) }

            val response =
                client.post("/api/v1/organizations") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("{")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    private suspend fun io.ktor.client.statement.HttpResponse.organizationRole() =
        Json.parseToJsonElement(bodyAsText()).jsonObject.getValue("role").jsonPrimitive.content
}

private class FakeOrganizationOperations : OrganizationOperations {
    var lastUserId: UUID? = null
    var createRequest: CreateOrganizationRequest? = null
    var updateRequest: UpdateOrganizationRequest? = null

    override suspend fun create(userId: UUID, request: CreateOrganizationRequest): OrganizationResponse {
        lastUserId = userId
        createRequest = request
        return response(name = request.name, slug = request.slug)
    }

    override suspend fun current(userId: UUID): OrganizationResponse {
        lastUserId = userId
        return response()
    }

    override suspend fun update(userId: UUID, request: UpdateOrganizationRequest): OrganizationResponse {
        lastUserId = userId
        updateRequest = request
        return response(name = request.name ?: "Acme", slug = request.slug ?: "acme")
    }

    private fun response(name: String = "Acme", slug: String = "acme") =
        OrganizationResponse(
            id = UUID.randomUUID().toString(),
            name = name,
            slug = slug,
            role = "owner",
            createdAt = "2026-09-06T18:00:00Z",
            updatedAt = "2026-09-06T18:00:00Z",
        )
}
