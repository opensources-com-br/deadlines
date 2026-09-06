package deadlines.organizations.members

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import deadlines.organizations.access.RoleResponse
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
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

class MemberRoutesTest {
    private val tokens = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `member routes require authentication`() = testApplication {
        application { module(tokenService = tokens, memberService = FakeMemberOperations()) }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/members").status)
    }

    @Test
    fun `supports member HTTP lifecycle`() = testApplication {
        val service = FakeMemberOperations()
        val token = tokens.issue(UUID.randomUUID()).accessToken
        application { module(tokenService = tokens, memberService = service) }

        assertEquals(HttpStatusCode.OK, client.get("/api/v1/members") { bearerAuth(token) }.status)
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/members/${service.id}") { bearerAuth(token) }.status)
        assertEquals(
            HttpStatusCode.OK,
            client.patch("/api/v1/members/${service.id}") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"roleId":"${service.roleId}"}""")
            }.status,
        )
        assertEquals(service.roleId.toString(), service.updatedRoleId)
        assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/members/${service.id}") { bearerAuth(token) }.status)
    }
}

private class FakeMemberOperations : MemberOperations {
    val id = UUID.randomUUID()
    val roleId = UUID.randomUUID()
    var updatedRoleId: String? = null

    override suspend fun list(userId: UUID) = MemberListResponse(listOf(member()))
    override suspend fun get(userId: UUID, membershipId: UUID) = member()
    override suspend fun updateRole(userId: UUID, membershipId: UUID, request: UpdateMemberRoleRequest): MemberResponse {
        updatedRoleId = request.roleId
        return member()
    }
    override suspend fun remove(userId: UUID, membershipId: UUID) = Unit

    private fun member() =
        MemberResponse(
            id.toString(),
            UUID.randomUUID().toString(),
            "member@example.com",
            "Test",
            "Member",
            RoleResponse(roleId.toString(), "member", "Member", null, true, "2026-09-06T18:00:00Z", "2026-09-06T18:00:00Z"),
            "2026-09-06T18:00:00Z",
        )
}
