package opensources.modules.billing.plans

import opensources.application.module
import opensources.config.AuthConfig
import opensources.identity.auth.TokenService
import opensources.modules.billing.BillingModule
import opensources.modules.billing.subscriptions.SubscriptionOperations
import opensources.modules.billing.subscriptions.SubscriptionResponse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanRoutesTest {
    private val tokenService = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `public plan catalog does not require authentication`() = testApplication {
        val plan =
            Plan(
                id = UUID.randomUUID(),
                key = "free",
                name = "Free",
                description = "For getting started",
                monthlyPriceCents = 0,
                currency = "USD",
                limits = listOf(PlanLimit("members", 3)),
            )
        application {
            module(
                tokenService = tokenService,
                billing = BillingModule(
                    plans = PlanService(RoutePlans(listOf(plan))),
                    subscriptions = object : SubscriptionOperations {
                        override suspend fun current(userId: UUID): SubscriptionResponse = error("not used")
                    },
                ),
            )
        }

        val response = client.get("/api/v1/plans")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "{\"data\":[{\"id\":\"${plan.id}\",\"key\":\"free\",\"name\":\"Free\"," +
                "\"description\":\"For getting started\",\"monthlyPriceCents\":0,\"currency\":\"USD\"," +
                "\"limits\":[{\"resource\":\"members\",\"value\":3}]}]}",
            response.bodyAsText(),
        )
    }
}

private class RoutePlans(private val values: List<Plan>) : PlanRepository {
    override suspend fun listActive(): List<Plan> = values
}
