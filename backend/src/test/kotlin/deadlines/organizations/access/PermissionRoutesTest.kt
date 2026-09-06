package deadlines.organizations.access

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionRoutesTest {
    private val tokens = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `permission routes require authentication`() =
        testApplication {
            application { module(tokenService = tokens, permissionService = FakePermissionOperations()) }
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/permissions").status)
            assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/permissions").status)
        }

    @Test
    fun `supports the permission HTTP lifecycle`() =
        testApplication {
            val service = FakePermissionOperations()
            val token = tokens.issue(UUID.randomUUID()).accessToken
            application { module(tokenService = tokens, permissionService = service) }

            assertEquals(HttpStatusCode.OK, client.get("/api/v1/permissions") { bearerAuth(token) }.status)
            val created =
                client.post("/api/v1/permissions") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"key":"deadline.manage","name":"Manage deadlines"}""")
                }
            assertEquals(HttpStatusCode.Created, created.status)
            assertEquals("/api/v1/permissions/${service.id}", created.headers[HttpHeaders.Location])
            assertEquals("deadline.manage", service.created?.key)

            assertEquals(HttpStatusCode.OK, client.get("/api/v1/permissions/${service.id}") { bearerAuth(token) }.status)
            assertEquals(
                HttpStatusCode.OK,
                client.patch("/api/v1/permissions/${service.id}") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Updated"}""")
                }.status,
            )
            assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/permissions/${service.id}") { bearerAuth(token) }.status)
        }

    @Test
    fun `rejects malformed permission identifiers`() =
        testApplication {
            val token = tokens.issue(UUID.randomUUID()).accessToken
            application { module(tokenService = tokens, permissionService = FakePermissionOperations()) }
            assertEquals(
                HttpStatusCode.UnprocessableEntity,
                client.get("/api/v1/permissions/not-a-uuid") { bearerAuth(token) }.status,
            )
        }
}

private class FakePermissionOperations : PermissionOperations {
    val id: UUID = UUID.randomUUID()
    var created: CreatePermissionRequest? = null

    override suspend fun list(userId: UUID) = PermissionListResponse(listOf(response()))

    override suspend fun get(userId: UUID, permissionId: UUID) = response()

    override suspend fun create(userId: UUID, request: CreatePermissionRequest): PermissionResponse {
        created = request
        return response()
    }

    override suspend fun update(userId: UUID, permissionId: UUID, request: UpdatePermissionRequest) = response()

    override suspend fun delete(userId: UUID, permissionId: UUID) = Unit

    private fun response() =
        PermissionResponse(id.toString(), "deadline.manage", "Manage deadlines", null, false, "2026-09-06T18:00:00Z", "2026-09-06T18:00:00Z")
}
