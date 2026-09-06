package deadlines.organizations.audits

import deadlines.shared.database.DatabaseQuery
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.selectAll

class ExposedAuditRepository(private val query: DatabaseQuery) : AuditRepository {
    override suspend fun list(organizationId: UUID, filter: AuditFilter): AuditListResponse = query {
        val rows = AuditLogs.selectAll().where {
            (AuditLogs.organizationId eq organizationId) and
                (filter.action?.let { AuditLogs.action eq it } ?: Op.TRUE) and
                (filter.resource?.let { AuditLogs.resource eq it } ?: Op.TRUE) and
                (filter.actorId?.let { AuditLogs.actorId eq it } ?: Op.TRUE) and
                (filter.resourceId?.let { AuditLogs.resourceId eq it } ?: Op.TRUE) and
                (filter.from?.let { AuditLogs.occurredAt greaterEq it.atOffset(ZoneOffset.UTC) } ?: Op.TRUE) and
                (filter.to?.let { AuditLogs.occurredAt lessEq it.atOffset(ZoneOffset.UTC) } ?: Op.TRUE)
        }.orderBy(AuditLogs.occurredAt to SortOrder.DESC, AuditLogs.id to SortOrder.DESC)
            .offset(filter.offset).limit(filter.limit + 1).map {
                AuditResponse(
                    it[AuditLogs.id].toString(), it[AuditLogs.organizationId].toString(),
                    it[AuditLogs.actorId]?.toString(), it[AuditLogs.occurredAt].toInstant().toString(),
                    it[AuditLogs.action], it[AuditLogs.resource], it[AuditLogs.resourceId].toString(),
                    Json.parseToJsonElement(it[AuditLogs.metadata]).jsonObject,
                )
            }
        AuditListResponse(rows.take(filter.limit), filter.offset, filter.limit, rows.size > filter.limit)
    }
}

private object AuditLogs : Table("organization_audit_logs") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id")
    val actorId = javaUUID("actor_id").nullable()
    val occurredAt = timestampWithTimeZone("occurred_at")
    val action = varchar("action", 80)
    val resource = varchar("resource", 40)
    val resourceId = javaUUID("resource_id")
    // Read-only mapping: PostgreSQL returns the JSONB representation as text.
    val metadata = text("metadata")
}
