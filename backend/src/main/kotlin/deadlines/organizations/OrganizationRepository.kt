package deadlines.organizations

import deadlines.shared.database.DatabaseQuery
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

interface OrganizationRepository {
    suspend fun createWithOwner(context: OrganizationContext): OrganizationContext

    suspend fun findCurrentByUser(userId: UUID): OrganizationContext?

    suspend fun findRetainedByUser(userId: UUID): OrganizationContext? = findCurrentByUser(userId)

    suspend fun update(organization: Organization): Organization

    suspend fun suspend(organizationId: UUID, updatedAt: Instant): Boolean = false

    suspend fun reactivateOwnedBy(userId: UUID, updatedAt: Instant): OrganizationContext? = null

    suspend fun delete(organizationId: UUID, deletedAt: Instant): Boolean = false
}

class ExposedOrganizationRepository(
    private val query: DatabaseQuery,
) : OrganizationRepository {
    override suspend fun createWithOwner(context: OrganizationContext): OrganizationContext =
        mapOrganizationConflict {
            query {
                OrganizationsTable.insert {
                    it[id] = context.organization.id
                    it[name] = context.organization.name
                    it[slug] = context.organization.slug
                    it[createdBy] = context.organization.createdBy
                    it[createdAt] = context.organization.createdAt.atOffset(ZoneOffset.UTC)
                    it[updatedAt] = context.organization.updatedAt.atOffset(ZoneOffset.UTC)
                    it[status] = context.organization.status.name.lowercase()
                    it[deletedAt] = context.organization.deletedAt?.atOffset(ZoneOffset.UTC)
                }
                OrganizationMembershipsTable.insert {
                    it[id] = context.membership.id
                    it[organizationId] = context.membership.organizationId
                    it[userId] = context.membership.userId
                    it[role] = context.membership.role.name.lowercase()
                    it[status] = context.membership.status.name.lowercase()
                    it[joinedAt] = context.membership.joinedAt.atOffset(ZoneOffset.UTC)
                    it[removedAt] = context.membership.removedAt?.atOffset(ZoneOffset.UTC)
                }
                context
            }
        }

    override suspend fun findCurrentByUser(userId: UUID): OrganizationContext? =
        query {
            organizationContextQuery()
                .where {
                    (OrganizationMembershipsTable.userId eq userId) and
                        (OrganizationMembershipsTable.status eq MembershipStatus.ACTIVE.name.lowercase()) and
                        (OrganizationsTable.status eq OrganizationStatus.ACTIVE.name.lowercase())
                }
                .singleOrNull()
                ?.toOrganizationContext()
        }

    override suspend fun findRetainedByUser(userId: UUID): OrganizationContext? =
        query {
            organizationContextQuery()
                .where {
                    (OrganizationMembershipsTable.userId eq userId) and
                        (OrganizationMembershipsTable.status inList listOf("active", "suspended")) and
                        (OrganizationsTable.status inList listOf("active", "suspended"))
                }
                .singleOrNull()
                ?.toOrganizationContext()
        }

    override suspend fun update(organization: Organization): Organization =
        mapOrganizationConflict {
            query {
                OrganizationsTable.update({ OrganizationsTable.id eq organization.id }) {
                    it[name] = organization.name
                    it[slug] = organization.slug
                    it[updatedAt] = organization.updatedAt.atOffset(ZoneOffset.UTC)
                }
                organization
            }
        }

    override suspend fun suspend(organizationId: UUID, updatedAt: Instant): Boolean =
        query {
            OrganizationsTable.update({
                (OrganizationsTable.id eq organizationId) and
                    (OrganizationsTable.status eq ACTIVE_ORGANIZATION_STATUS)
            }) {
                it[status] = SUSPENDED_ORGANIZATION_STATUS
                it[OrganizationsTable.updatedAt] = updatedAt.atOffset(ZoneOffset.UTC)
            } == 1
        }

    override suspend fun reactivateOwnedBy(userId: UUID, updatedAt: Instant): OrganizationContext? =
        query {
            val context = organizationContextQuery()
                .where {
                    (OrganizationMembershipsTable.userId eq userId) and
                        (OrganizationMembershipsTable.role eq OWNER_ROLE) and
                        (OrganizationMembershipsTable.status eq ACTIVE_MEMBERSHIP_STATUS) and
                        (OrganizationsTable.status eq SUSPENDED_ORGANIZATION_STATUS)
                }
                .singleOrNull()
                ?.toOrganizationContext()
                ?: return@query null
            val updated = OrganizationsTable.update({
                (OrganizationsTable.id eq context.organization.id) and
                    (OrganizationsTable.status eq SUSPENDED_ORGANIZATION_STATUS)
            }) {
                it[status] = ACTIVE_ORGANIZATION_STATUS
                it[OrganizationsTable.updatedAt] = updatedAt.atOffset(ZoneOffset.UTC)
            }
            context.takeIf { updated == 1 }?.copy(
                organization = context.organization.copy(status = OrganizationStatus.ACTIVE, updatedAt = updatedAt),
            )
        }

    override suspend fun delete(organizationId: UUID, deletedAt: Instant): Boolean =
        query {
            val organizationUpdated = OrganizationsTable.update({
                (OrganizationsTable.id eq organizationId) and
                    (OrganizationsTable.status eq ACTIVE_ORGANIZATION_STATUS)
            }) {
                it[status] = DELETED_ORGANIZATION_STATUS
                it[OrganizationsTable.deletedAt] = deletedAt.atOffset(ZoneOffset.UTC)
                it[OrganizationsTable.updatedAt] = deletedAt.atOffset(ZoneOffset.UTC)
            }
            if (organizationUpdated != 1) return@query false

            OrganizationMembershipsTable.update({
                (OrganizationMembershipsTable.organizationId eq organizationId) and
                    (OrganizationMembershipsTable.status inList RETAINED_MEMBERSHIP_STATUSES)
            }) {
                it[status] = REMOVED_MEMBERSHIP_STATUS
                it[removedAt] = deletedAt.atOffset(ZoneOffset.UTC)
            }
            OrganizationInvitationsTable.update({
                (OrganizationInvitationsTable.organizationId eq organizationId) and
                    (OrganizationInvitationsTable.status eq PENDING_INVITATION_STATUS)
            }) {
                it[status] = REVOKED_INVITATION_STATUS
                it[revokedAt] = deletedAt.atOffset(ZoneOffset.UTC)
                it[updatedAt] = deletedAt.atOffset(ZoneOffset.UTC)
            }
            true
        }
}

private suspend fun <T> mapOrganizationConflict(block: suspend () -> T): T =
    try {
        block()
    } catch (exception: Exception) {
        if (exception.hasConstraint(ONE_ACTIVE_MEMBERSHIP_CONSTRAINT)) {
            throw ActiveMembershipAlreadyExistsException()
        }
        if (exception.hasSqlState(UNIQUE_VIOLATION_SQL_STATE)) {
            throw OrganizationAlreadyExistsException()
        }
        throw exception
    }

private fun Throwable.hasSqlState(sqlState: String): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == sqlState }

private fun Throwable.hasConstraint(constraint: String): Boolean =
    generateSequence(this) { it.cause }
        .any { it.message?.contains(constraint) == true }

private const val UNIQUE_VIOLATION_SQL_STATE = "23505"
private const val ONE_ACTIVE_MEMBERSHIP_CONSTRAINT = "organization_memberships_one_retained_per_user"
private const val ACTIVE_ORGANIZATION_STATUS = "active"
private const val SUSPENDED_ORGANIZATION_STATUS = "suspended"
private const val DELETED_ORGANIZATION_STATUS = "deleted"
private const val ACTIVE_MEMBERSHIP_STATUS = "active"
private const val REMOVED_MEMBERSHIP_STATUS = "removed"
private const val OWNER_ROLE = "owner"
private const val PENDING_INVITATION_STATUS = "pending"
private const val REVOKED_INVITATION_STATUS = "revoked"
private val RETAINED_MEMBERSHIP_STATUSES = listOf("active", "suspended")

private object OrganizationUsersTable : Table("users") {
    val id = javaUUID("id")
}

private object OrganizationsTable : Table("organizations") {
    val id = javaUUID("id")
    val name = varchar("name", 160)
    val slug = varchar("slug", 80)
    val createdBy = javaUUID("created_by").references(OrganizationUsersTable.id)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val status = varchar("status", 32)
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

private object OrganizationMembershipsTable : Table("organization_memberships") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(OrganizationsTable.id)
    val userId = javaUUID("user_id").references(OrganizationUsersTable.id)
    val role = varchar("role", 32)
    val status = varchar("status", 32)
    val joinedAt = timestampWithTimeZone("joined_at")
    val removedAt = timestampWithTimeZone("removed_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

private object OrganizationInvitationsTable : Table("organization_invitations") {
    val organizationId = javaUUID("organization_id").references(OrganizationsTable.id)
    val status = varchar("status", 32)
    val updatedAt = timestampWithTimeZone("updated_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
}

private fun organizationContextQuery() =
    (OrganizationsTable innerJoin OrganizationMembershipsTable).selectAll()

private fun org.jetbrains.exposed.v1.core.ResultRow.toOrganizationContext() =
    OrganizationContext(
        organization =
            Organization(
                id = this[OrganizationsTable.id],
                name = this[OrganizationsTable.name],
                slug = this[OrganizationsTable.slug],
                createdBy = this[OrganizationsTable.createdBy],
                createdAt = this[OrganizationsTable.createdAt].toInstant(),
                updatedAt = this[OrganizationsTable.updatedAt].toInstant(),
                status = OrganizationStatus.valueOf(this[OrganizationsTable.status].uppercase()),
                deletedAt = this[OrganizationsTable.deletedAt]?.toInstant(),
            ),
        membership =
            OrganizationMembership(
                id = this[OrganizationMembershipsTable.id],
                organizationId = this[OrganizationMembershipsTable.organizationId],
                userId = this[OrganizationMembershipsTable.userId],
                role = MembershipRole.valueOf(this[OrganizationMembershipsTable.role].uppercase()),
                status = MembershipStatus.valueOf(this[OrganizationMembershipsTable.status].uppercase()),
                joinedAt = this[OrganizationMembershipsTable.joinedAt].toInstant(),
                removedAt = this[OrganizationMembershipsTable.removedAt]?.toInstant(),
            ),
    )
