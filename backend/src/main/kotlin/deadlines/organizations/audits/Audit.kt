package deadlines.organizations.audits

import java.time.Instant
import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Propagates across coroutine dispatcher switches; never shared between requests. */
class AuditActor(val userId: UUID) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<AuditActor>
}

suspend fun <T> withAuditActor(userId: UUID, block: suspend () -> T): T =
    withContext(AuditActor(userId)) { block() }

data class AuditFilter(
    val offset: Long = 0,
    val limit: Int = 20,
    val action: String? = null,
    val resource: String? = null,
    val actorId: UUID? = null,
    val resourceId: UUID? = null,
    val from: Instant? = null,
    val to: Instant? = null,
)

@Serializable
data class AuditResponse(
    val id: String,
    val organizationId: String,
    val actorId: String?,
    val occurredAt: String,
    val action: String,
    val resource: String,
    val resourceId: String,
    val metadata: JsonObject,
)

@Serializable
data class AuditListResponse(val data: List<AuditResponse>, val offset: Long, val limit: Int, val hasMore: Boolean)

interface AuditRepository {
    suspend fun list(organizationId: UUID, filter: AuditFilter): AuditListResponse
}
