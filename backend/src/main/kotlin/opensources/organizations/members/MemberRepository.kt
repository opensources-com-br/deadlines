package opensources.organizations.members

import opensources.organizations.access.Role
import opensources.organizations.MembershipStatus
import opensources.shared.database.DatabaseQuery
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

interface MemberRepository {
    suspend fun list(organizationId: UUID): List<OrganizationMember>

    suspend fun findById(organizationId: UUID, membershipId: UUID): OrganizationMember?

    suspend fun findByUserId(organizationId: UUID, userId: UUID): OrganizationMember?

    suspend fun updateRole(organizationId: UUID, membershipId: UUID, roleId: UUID): Boolean

    suspend fun suspend(organizationId: UUID, membershipId: UUID): Boolean = false

    suspend fun reactivate(organizationId: UUID, membershipId: UUID): Boolean = false

    suspend fun transferOwnership(
        organizationId: UUID,
        currentOwnerMembershipId: UUID,
        nextOwnerMembershipId: UUID,
        ownerRoleId: UUID,
        previousOwnerRoleId: UUID,
    ): Boolean = false

    suspend fun remove(organizationId: UUID, membershipId: UUID, removedAt: Instant): Boolean
}

class ExposedMemberRepository(
    private val query: DatabaseQuery,
) : MemberRepository {
    override suspend fun list(organizationId: UUID): List<OrganizationMember> =
        query {
            memberQuery()
                .where {
                    (MembershipsTable.organizationId eq organizationId) and
                        (MembershipsTable.status inList RETAINED_STATUSES)
                }
                .orderBy(ProfilesTable.firstName to SortOrder.ASC, ProfilesTable.lastName to SortOrder.ASC)
                .map { it.toMember() }
        }

    override suspend fun findById(organizationId: UUID, membershipId: UUID): OrganizationMember? =
        query {
            memberQuery()
                .where {
                    (MembershipsTable.organizationId eq organizationId) and
                        (MembershipsTable.id eq membershipId) and
                        (MembershipsTable.status inList RETAINED_STATUSES)
                }
                .singleOrNull()
                ?.toMember()
        }

    override suspend fun findByUserId(organizationId: UUID, userId: UUID): OrganizationMember? =
        query {
            memberQuery()
                .where {
                    (MembershipsTable.organizationId eq organizationId) and
                        (MembershipsTable.userId eq userId) and
                        (MembershipsTable.status inList RETAINED_STATUSES)
                }
                .singleOrNull()
                ?.toMember()
        }

    override suspend fun suspend(organizationId: UUID, membershipId: UUID): Boolean =
        updateStatus(organizationId, membershipId, ACTIVE_STATUS, SUSPENDED_STATUS)

    override suspend fun reactivate(organizationId: UUID, membershipId: UUID): Boolean =
        updateStatus(organizationId, membershipId, SUSPENDED_STATUS, ACTIVE_STATUS)

    override suspend fun updateRole(organizationId: UUID, membershipId: UUID, roleId: UUID): Boolean =
        query {
            MembershipsTable.update({
                (MembershipsTable.organizationId eq organizationId) and
                    (MembershipsTable.id eq membershipId) and
                    (MembershipsTable.status inList RETAINED_STATUSES)
            }) {
                it[MembershipsTable.roleId] = roleId
            } == 1
        }

    override suspend fun remove(organizationId: UUID, membershipId: UUID, removedAt: Instant): Boolean =
        query {
            MembershipsTable.update({
                (MembershipsTable.organizationId eq organizationId) and
                    (MembershipsTable.id eq membershipId) and
                    (MembershipsTable.status inList RETAINED_STATUSES)
            }) {
                it[status] = "removed"
                it[MembershipsTable.removedAt] = removedAt.atOffset(ZoneOffset.UTC)
            } == 1
        }

    override suspend fun transferOwnership(
        organizationId: UUID,
        currentOwnerMembershipId: UUID,
        nextOwnerMembershipId: UUID,
        ownerRoleId: UUID,
        previousOwnerRoleId: UUID,
    ): Boolean =
        query {
            val eligibleIds =
                MembershipsTable.selectAll()
                    .where {
                        (MembershipsTable.organizationId eq organizationId) and
                            (MembershipsTable.id inList listOf(currentOwnerMembershipId, nextOwnerMembershipId)) and
                            (MembershipsTable.status eq ACTIVE_STATUS)
                    }
                    .map { it[MembershipsTable.id] }
                    .toSet()
            if (eligibleIds != setOf(currentOwnerMembershipId, nextOwnerMembershipId)) return@query false

            val previousOwnerUpdated = MembershipsTable.update({
                (MembershipsTable.organizationId eq organizationId) and
                    (MembershipsTable.id eq currentOwnerMembershipId) and
                    (MembershipsTable.status eq ACTIVE_STATUS)
            }) {
                it[roleId] = previousOwnerRoleId
                it[legacyRole] = MEMBER_ROLE
            }
            val nextOwnerUpdated = MembershipsTable.update({
                (MembershipsTable.organizationId eq organizationId) and
                    (MembershipsTable.id eq nextOwnerMembershipId) and
                    (MembershipsTable.status eq ACTIVE_STATUS)
            }) {
                it[roleId] = ownerRoleId
                it[legacyRole] = OWNER_ROLE
            }
            nextOwnerUpdated == 1 && previousOwnerUpdated == 1
        }


    private suspend fun updateStatus(
        organizationId: UUID,
        membershipId: UUID,
        currentStatus: String,
        nextStatus: String,
    ): Boolean =
        query {
            MembershipsTable.update({
                (MembershipsTable.organizationId eq organizationId) and
                    (MembershipsTable.id eq membershipId) and
                    (MembershipsTable.status eq currentStatus)
            }) {
                it[status] = nextStatus
            } == 1
        }
}

private const val ACTIVE_STATUS = "active"
private const val SUSPENDED_STATUS = "suspended"
private const val OWNER_ROLE = "owner"
private const val MEMBER_ROLE = "member"
private val RETAINED_STATUSES = listOf(ACTIVE_STATUS, SUSPENDED_STATUS)

private object MemberOrganizationsTable : Table("organizations") {
    val id = javaUUID("id")
}

private object UsersTable : Table("users") {
    val id = javaUUID("id")
    val email = varchar("email", 320)
}

private object ProfilesTable : Table("user_profiles") {
    val userId = javaUUID("user_id").references(UsersTable.id)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
}

private object RolesTable : Table("roles") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(MemberOrganizationsTable.id)
    val key = varchar("key", 80)
    val name = varchar("name", 120)
    val description = text("description").nullable()
    val isSystem = bool("is_system")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

private object MembershipsTable : Table("organization_memberships") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(MemberOrganizationsTable.id)
    val userId = javaUUID("user_id").references(UsersTable.id)
    val roleId = javaUUID("role_id").references(RolesTable.id)
    val legacyRole = varchar("role", 32)
    val status = varchar("status", 32)
    val joinedAt = timestampWithTimeZone("joined_at")
    val removedAt = timestampWithTimeZone("removed_at").nullable()
}

private fun memberQuery() =
    (((MembershipsTable innerJoin UsersTable) innerJoin ProfilesTable) innerJoin RolesTable).selectAll()

private fun org.jetbrains.exposed.v1.core.ResultRow.toMember() =
    OrganizationMember(
        membershipId = this[MembershipsTable.id],
        organizationId = this[MembershipsTable.organizationId],
        userId = this[MembershipsTable.userId],
        email = this[UsersTable.email],
        firstName = this[ProfilesTable.firstName],
        lastName = this[ProfilesTable.lastName],
        role =
            Role(
                id = this[RolesTable.id],
                organizationId = this[RolesTable.organizationId],
                key = this[RolesTable.key],
                name = this[RolesTable.name],
                description = this[RolesTable.description],
                isSystem = this[RolesTable.isSystem],
                createdAt = this[RolesTable.createdAt].toInstant(),
                updatedAt = this[RolesTable.updatedAt].toInstant(),
            ),
        status = MembershipStatus.valueOf(this[MembershipsTable.status].uppercase()),
        joinedAt = this[MembershipsTable.joinedAt].toInstant(),
    )
