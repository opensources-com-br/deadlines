package deadlines.organizations.invitations

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import deadlines.organizations.access.RoleResponse
import deadlines.organizations.members.MemberResponse
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
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

class InvitationRoutesTest {
    private val tokens = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `preview is public while invitation management requires authentication`() = testApplication {
        application { module(tokenService = tokens, invitationService = FakeInvitationOperations()) }

        assertEquals(HttpStatusCode.OK, client.get("/api/v1/invitations/preview?token=raw-token").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/invitations").status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/invitations/accept").status)
    }

    @Test
    fun `supports invitation HTTP lifecycle`() = testApplication {
        val service = FakeInvitationOperations()
        val token = tokens.issue(UUID.randomUUID()).accessToken
        application { module(tokenService = tokens, invitationService = service) }

        assertEquals(HttpStatusCode.OK, client.get("/api/v1/invitations") { bearerAuth(token) }.status)
        val created = client.post("/api/v1/invitations") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"email":"invitee@example.com","roleId":"${service.roleId}"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status)
        assertEquals("/api/v1/invitations/${service.id}", created.headers[HttpHeaders.Location])
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/invitations/${service.id}") { bearerAuth(token) }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/invitations/${service.id}/resend") { bearerAuth(token) }.status)
        assertEquals(
            HttpStatusCode.OK,
            client.post("/api/v1/invitations/accept") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"token":"raw-token"}""")
            }.status,
        )
        assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/invitations/${service.id}") { bearerAuth(token) }.status)
    }
}

private class FakeInvitationOperations : InvitationOperations {
    val id = UUID.randomUUID()
    val roleId = UUID.randomUUID()

    override suspend fun list(userId: UUID) = InvitationListResponse(listOf(invitation()))
    override suspend fun get(userId: UUID, invitationId: UUID) = invitation()
    override suspend fun create(userId: UUID, request: CreateInvitationRequest) = invitation()
    override suspend fun resend(userId: UUID, invitationId: UUID) = invitation()
    override suspend fun revoke(userId: UUID, invitationId: UUID) = Unit
    override suspend fun preview(rawToken: String) =
        InvitationPreviewResponse("Acme", "invitee@example.com", "Member", "pending", "2026-09-13T18:00:00Z")
    override suspend fun accept(userId: UUID, rawToken: String) = member()

    private fun invitation() =
        InvitationResponse(
            id.toString(),
            UUID.randomUUID().toString(),
            "Acme",
            "invitee@example.com",
            role(),
            "pending",
            "2026-09-13T18:00:00Z",
            "2026-09-06T18:00:00Z",
            "2026-09-06T18:00:00Z",
        )

    private fun member() =
        MemberResponse(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "invitee@example.com",
            "Test",
            "User",
            role(),
            "2026-09-06T18:00:00Z",
        )

    private fun role() =
        RoleResponse(roleId.toString(), "member", "Member", null, true, "2026-09-06T18:00:00Z", "2026-09-06T18:00:00Z")
}
