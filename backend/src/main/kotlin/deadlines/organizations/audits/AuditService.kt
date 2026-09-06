package deadlines.organizations.audits

import deadlines.organizations.MembershipRole
import deadlines.organizations.OrganizationAccessDeniedException
import deadlines.organizations.OrganizationNotFoundException
import deadlines.organizations.OrganizationRepository
import deadlines.shared.errors.ApiException
import java.time.Instant
import java.util.UUID

class AuditValidationException(field: String) : ApiException(
    422, "VALIDATION_ERROR", "Invalid audit filter", mapOf(field to "invalid value"),
)

class AuditService(private val organizations: OrganizationRepository, private val audits: AuditRepository) {
    suspend fun list(userId: UUID, parameters: Map<String, String>): AuditListResponse {
        val context = organizations.findCurrentByUser(userId) ?: throw OrganizationNotFoundException()
        if (context.membership.role != MembershipRole.OWNER) throw OrganizationAccessDeniedException()
        val allowed = setOf("offset", "limit", "action", "resource", "actorId", "resourceId", "from", "to")
        parameters.keys.firstOrNull { it !in allowed }?.let { throw AuditValidationException(it) }
        fun number(key: String, default: Long, range: LongRange): Long = parameters[key]?.let {
            it.toLongOrNull()?.takeIf { value -> value in range } ?: throw AuditValidationException(key)
        } ?: default
        fun uuid(key: String): UUID? = parameters[key]?.let {
            runCatching { UUID.fromString(it).also { value -> require(value.toString().equals(it, ignoreCase = true)) } }.getOrElse { throw AuditValidationException(key) }
        }
        fun instant(key: String): Instant? = parameters[key]?.let {
            runCatching { Instant.parse(it).also { value ->
                require(value >= Instant.parse("0001-01-01T00:00:00Z") && value < Instant.parse("+10000-01-01T00:00:00Z"))
            } }.getOrElse { throw AuditValidationException(key) }
        }
        fun text(key: String, max: Int): String? = parameters[key]?.also {
            if (it.length !in 1..max || !Regex("^[a-z][a-z._]*$").matches(it)) throw AuditValidationException(key)
        }
        val filter = AuditFilter(
            offset = number("offset", 0, 0L..1_000_000L),
            limit = number("limit", 20, 1L..100L).toInt(),
            action = text("action", 80), resource = text("resource", 40),
            actorId = uuid("actorId"), resourceId = uuid("resourceId"),
            from = instant("from"), to = instant("to"),
        )
        if (filter.from != null && filter.to != null && filter.from > filter.to) throw AuditValidationException("to")
        return audits.list(context.organization.id, filter)
    }
}
