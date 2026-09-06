package deadlines.plans

import deadlines.application.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanRoutesTest {
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
        application { module(planService = PlanService(RoutePlans(listOf(plan)))) }

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
