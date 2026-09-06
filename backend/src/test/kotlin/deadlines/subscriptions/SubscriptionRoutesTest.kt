package deadlines.subscriptions

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionRoutesTest {
    private val tokenService = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `current subscription requires authentication and uses the current user`() = testApplication {
        val service = RouteSubscriptions()
        val userId = UUID.randomUUID()
        application { module(tokenService = tokenService, subscriptionService = service) }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/subscriptions/current").status)
        assertEquals(
            HttpStatusCode.OK,
            client.get("/api/v1/subscriptions/current") { bearerAuth(tokenService.issue(userId).accessToken) }.status,
        )
        assertEquals(userId, service.userId)
    }
}

private class RouteSubscriptions : SubscriptionOperations {
    var userId: UUID? = null

    override suspend fun current(userId: UUID): SubscriptionResponse {
        this.userId = userId
        return SubscriptionResponse(
            id = UUID.randomUUID().toString(),
            organizationId = UUID.randomUUID().toString(),
            status = "active",
            startedAt = "2026-09-06T20:00:00Z",
            endedAt = null,
            plan = deadlines.plans.PlanResponse(UUID.randomUUID().toString(), "free", "Free", null, 0, "USD", emptyList()),
        )
    }
}
