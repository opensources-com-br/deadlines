package deadlines.organizations

import deadlines.shared.database.DatabaseQuery
import java.sql.SQLException
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

interface OrganizationRepository {
    suspend fun createWithOwner(context: OrganizationContext): OrganizationContext

    suspend fun findCurrentByUser(userId: UUID): OrganizationContext?

    suspend fun update(organization: Organization): Organization
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
                        (OrganizationMembershipsTable.status eq MembershipStatus.ACTIVE.name.lowercase())
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
private const val ONE_ACTIVE_MEMBERSHIP_CONSTRAINT = "organization_memberships_one_active_per_user"

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
