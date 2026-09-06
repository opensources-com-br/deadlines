package deadlines.organizations.audits

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import deadlines.organizations.MembershipRole
import deadlines.organizations.access.TestOrganizationRepository
import deadlines.organizations.access.accessContext
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditRoutesTest {
    private val tokens = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `authentication owner access and current organization are enforced`() = testApplication {
        val owner = UUID.randomUUID()
        val organizations = TestOrganizationRepository(accessContext(owner))
        val repository = CapturingAudits()
        application { module(tokenService = tokens, auditService = AuditService(organizations, repository)) }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/audits").status)
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/audits") { bearerAuth(tokens.issue(owner).accessToken) }.status)
        assertEquals(organizations.context!!.organization.id, repository.organizationId)
        organizations.context = accessContext(owner, membershipRole = MembershipRole.MEMBER)
        assertEquals(HttpStatusCode.Forbidden, client.get("/api/v1/audits") { bearerAuth(tokens.issue(owner).accessToken) }.status)
        organizations.context = null
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/audits") { bearerAuth(tokens.issue(owner).accessToken) }.status)
        assertEquals(1, repository.calls)
    }

    @Test
    fun `rejects malformed filters before reading history`() = testApplication {
        val owner = UUID.randomUUID()
        val repository = CapturingAudits()
        application { module(tokenService = tokens, auditService = AuditService(TestOrganizationRepository(accessContext(owner)), repository)) }
        for (query in listOf("limit=0", "limit=101", "offset=-1", "offset=9223372036854775808", "limit=no",
            "actorId=invalid", "resourceId=invalid", "from=yesterday", "to=tomorrow", "action=", "resource=",
            "from=2026-09-07T00:00:00Z&to=2026-09-06T00:00:00Z", "organizationId=${UUID.randomUUID()}", "limit=1&limit=2")) {
            assertEquals(HttpStatusCode.UnprocessableEntity, client.get("/api/v1/audits?$query") {
                bearerAuth(tokens.issue(owner).accessToken)
            }.status, query)
        }
        assertEquals(0, repository.calls)
    }

    @Test
    fun `passes typed filters and page bounds to repository`() = testApplication {
        val owner = UUID.randomUUID()
        val resource = UUID.randomUUID()
        val repository = CapturingAudits()
        application { module(tokenService = tokens, auditService = AuditService(TestOrganizationRepository(accessContext(owner)), repository)) }
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/audits?limit=3&offset=6&action=role.created&resource=role&actorId=$owner&resourceId=$resource&from=2026-09-06T00:00:00Z") {
            bearerAuth(tokens.issue(owner).accessToken)
        }.status)
        assertEquals(3, repository.filter!!.limit)
        assertEquals(6L, repository.filter!!.offset)
        assertEquals(owner, repository.filter!!.actorId)
        assertEquals(resource, repository.filter!!.resourceId)
        assertEquals("role.created", repository.filter!!.action)
        assertEquals("role", repository.filter!!.resource)
        assertEquals("2026-09-06T00:00:00Z", repository.filter!!.from.toString())
    }
}

private class CapturingAudits : AuditRepository {
    var calls = 0
    var organizationId: UUID? = null
    var filter: AuditFilter? = null
    override suspend fun list(organizationId: UUID, filter: AuditFilter): AuditListResponse {
        calls++
        this.organizationId = organizationId
        this.filter = filter
        return AuditListResponse(emptyList(), filter.offset, filter.limit, false)
    }
}
