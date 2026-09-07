package deadlines.organizations

import java.time.Instant
import java.util.UUID

data class Organization(
    val id: UUID,
    val name: String,
    val slug: String,
    val createdBy: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: OrganizationStatus = OrganizationStatus.ACTIVE,
    val deletedAt: Instant? = null,
)

enum class OrganizationStatus {
    ACTIVE,
    SUSPENDED,
    DELETED,
}

data class OrganizationMembership(
    val id: UUID,
    val organizationId: UUID,
    val userId: UUID,
    val role: MembershipRole,
    val status: MembershipStatus,
    val joinedAt: Instant,
    val removedAt: Instant?,
)

enum class MembershipRole {
    OWNER,
    MEMBER,
}

enum class MembershipStatus {
    ACTIVE,
    SUSPENDED,
    REMOVED,
}

data class OrganizationContext(
    val organization: Organization,
    val membership: OrganizationMembership,
)
