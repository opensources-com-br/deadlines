package deadlines.plans

import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanServiceTest {
    @Test
    fun `lists active plans with limits`() = runTest {
        val free = Plan(UUID.randomUUID(), "free", "Free", null, 0, "USD", listOf(PlanLimit("members", 3)))
        val response = PlanService(FakePlans(listOf(free))).list()

        assertEquals("free", response.data.single().key)
        assertEquals(3, response.data.single().limits.single().value)
    }
}

private class FakePlans(private val values: List<Plan>) : PlanRepository {
    override suspend fun listActive() = values
}
