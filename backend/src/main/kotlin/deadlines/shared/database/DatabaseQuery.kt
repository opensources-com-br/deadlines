package deadlines.shared.database

import deadlines.organizations.audits.AuditActor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class DatabaseQuery(
    private val database: Database,
) {
    suspend operator fun <T> invoke(block: () -> T): T =
        withContext(Dispatchers.IO) {
            val actorId = coroutineContext[AuditActor]?.userId
            transaction(database) {
                // UUID is typed, and SET LOCAL is cleared on commit/rollback.
                exec("SELECT set_config('deadlines.audit_actor', '${actorId ?: ""}', true)")

                block()
            }
        }
}
