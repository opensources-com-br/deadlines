package deadlines.organizations.access

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class RoleRoutesTest {
    private val tokens = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `role routes require authentication`() =
        testApplication {
            application { module(tokenService = tokens, roleService = FakeRoleOperations()) }
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/roles").status)
            assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/roles").status)
        }

    @Test
    fun `supports the role and permission assignment HTTP lifecycle`() =
        testApplication {
            val service = FakeRoleOperations()
            val token = tokens.issue(UUID.randomUUID()).accessToken
            application { module(tokenService = tokens, roleService = service) }

            assertEquals(HttpStatusCode.OK, client.get("/api/v1/roles") { bearerAuth(token) }.status)
            val created =
                client.post("/api/v1/roles") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"key":"manager","name":"Manager"}""")
                }
            assertEquals(HttpStatusCode.Created, created.status)
            assertEquals("/api/v1/roles/${service.id}", created.headers[HttpHeaders.Location])

            assertEquals(HttpStatusCode.OK, client.get("/api/v1/roles/${service.id}") { bearerAuth(token) }.status)
            assertEquals(
                HttpStatusCode.OK,
                client.patch("/api/v1/roles/${service.id}") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Updated"}""")
                }.status,
            )
            assertEquals(
                HttpStatusCode.OK,
                client.get("/api/v1/roles/${service.id}/permissions") { bearerAuth(token) }.status,
            )
            assertEquals(
                HttpStatusCode.OK,
                client.put("/api/v1/roles/${service.id}/permissions") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"permissionIds":["${service.permissionId}"]}""")
                }.status,
            )
            assertEquals(listOf(service.permissionId.toString()), service.replaced?.permissionIds)
            assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/roles/${service.id}") { bearerAuth(token) }.status)
        }
}

private class FakeRoleOperations : RoleOperations {
    val id: UUID = UUID.randomUUID()
    val permissionId: UUID = UUID.randomUUID()
    var replaced: ReplaceRolePermissionsRequest? = null

    override suspend fun list(userId: UUID) = RoleListResponse(listOf(role()))

    override suspend fun get(userId: UUID, roleId: UUID) = role()

    override suspend fun create(userId: UUID, request: CreateRoleRequest) = role()

    override suspend fun update(userId: UUID, roleId: UUID, request: UpdateRoleRequest) = role()

    override suspend fun delete(userId: UUID, roleId: UUID) = Unit

    override suspend fun listPermissions(userId: UUID, roleId: UUID) = permissions()

    override suspend fun replacePermissions(
        userId: UUID,
        roleId: UUID,
        request: ReplaceRolePermissionsRequest,
    ): PermissionListResponse {
        replaced = request
        return permissions()
    }

    private fun role() =
        RoleResponse(id.toString(), "manager", "Manager", null, false, "2026-09-06T18:00:00Z", "2026-09-06T18:00:00Z")

    private fun permissions() =
        PermissionListResponse(
            listOf(
                PermissionResponse(
                    permissionId.toString(),
                    "members.read",
                    "View members",
                    null,
                    true,
                    "2026-09-06T18:00:00Z",
                    "2026-09-06T18:00:00Z",
                ),
            ),
        )
}
