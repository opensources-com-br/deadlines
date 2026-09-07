package deadlines.organizations.members

import deadlines.organizations.access.Role
import deadlines.organizations.MembershipStatus
import java.time.Instant
import java.util.UUID

data class OrganizationMember(
    val membershipId: UUID,
    val organizationId: UUID,
    val userId: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: Role,
    val joinedAt: Instant,
    val status: MembershipStatus = MembershipStatus.ACTIVE,
)
