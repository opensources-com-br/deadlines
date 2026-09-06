package deadlines.plans

interface PlanOperations { suspend fun list(): PlanListResponse }

class PlanService(private val plans: PlanRepository) : PlanOperations {
    override suspend fun list() = PlanListResponse(plans.listActive().map(Plan::toResponse))
}
