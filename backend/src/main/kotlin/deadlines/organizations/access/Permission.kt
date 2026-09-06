package deadlines.organizations.access

import java.time.Instant
import java.util.UUID

data class Permission(
    val id: UUID,
    val organizationId: UUID?,
    val key: String,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
