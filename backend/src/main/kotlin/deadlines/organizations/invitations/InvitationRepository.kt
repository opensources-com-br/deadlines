package deadlines.organizations.invitations

import deadlines.organizations.ActiveMembershipAlreadyExistsException
import deadlines.organizations.access.Role
import deadlines.shared.database.DatabaseQuery
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

interface InvitationRepository {
    suspend fun list(organizationId: UUID, now: Instant): List<OrganizationInvitation>

    suspend fun findById(organizationId: UUID, invitationId: UUID, now: Instant): OrganizationInvitation?

    suspend fun findByTokenHash(tokenHash: String, now: Instant): OrganizationInvitation?

    suspend fun create(invitation: OrganizationInvitation): OrganizationInvitation

    suspend fun renew(organizationId: UUID, invitationId: UUID, tokenHash: String, expiresAt: Instant, now: Instant): Boolean

    suspend fun revoke(organizationId: UUID, invitationId: UUID, now: Instant): Boolean

    suspend fun accept(invitation: OrganizationInvitation, userId: UUID, membershipId: UUID, now: Instant): Boolean
}

class ExposedInvitationRepository(
    private val query: DatabaseQuery,
) : InvitationRepository {
    override suspend fun list(organizationId: UUID, now: Instant): List<OrganizationInvitation> =
        query {
            invitationQuery()
                .where { InvitationsTable.organizationId eq organizationId }
                .orderBy(InvitationsTable.createdAt to SortOrder.DESC)
                .map { it.toInvitation(now) }
        }

    override suspend fun findById(organizationId: UUID, invitationId: UUID, now: Instant): OrganizationInvitation? =
        query {
            invitationQuery()
                .where {
                    (InvitationsTable.organizationId eq organizationId) and
                        (InvitationsTable.id eq invitationId)
                }
                .singleOrNull()
                ?.toInvitation(now)
        }

    override suspend fun findByTokenHash(tokenHash: String, now: Instant): OrganizationInvitation? =
        query {
            invitationQuery()
                .where { InvitationsTable.tokenHash eq tokenHash }
                .singleOrNull()
                ?.toInvitation(now)
        }

    override suspend fun create(invitation: OrganizationInvitation): OrganizationInvitation =
        mapInvitationConflict {
            query {
                InvitationsTable.insert {
                    it[id] = invitation.id
                    it[organizationId] = invitation.organizationId
                    it[email] = invitation.email
                    it[roleId] = invitation.role.id
                    it[invitedBy] = invitation.invitedBy
                    it[tokenHash] = invitation.tokenHash
                    it[status] = PENDING_STATUS
                    it[expiresAt] = invitation.expiresAt.atOffset(ZoneOffset.UTC)
                    it[createdAt] = invitation.createdAt.atOffset(ZoneOffset.UTC)
                    it[updatedAt] = invitation.updatedAt.atOffset(ZoneOffset.UTC)
                }
                invitation
            }
        }

    override suspend fun renew(
        organizationId: UUID,
        invitationId: UUID,
        tokenHash: String,
        expiresAt: Instant,
        now: Instant,
    ): Boolean =
        query {
            InvitationsTable.update({
                (InvitationsTable.organizationId eq organizationId) and
                    (InvitationsTable.id eq invitationId) and
                    (InvitationsTable.status eq PENDING_STATUS)
            }) {
                it[InvitationsTable.tokenHash] = tokenHash
                it[InvitationsTable.expiresAt] = expiresAt.atOffset(ZoneOffset.UTC)
                it[updatedAt] = now.atOffset(ZoneOffset.UTC)
            } == 1
        }

    override suspend fun revoke(organizationId: UUID, invitationId: UUID, now: Instant): Boolean =
        query {
            InvitationsTable.update({
                (InvitationsTable.organizationId eq organizationId) and
                    (InvitationsTable.id eq invitationId) and
                    (InvitationsTable.status eq PENDING_STATUS)
            }) {
                it[status] = "revoked"
                it[updatedAt] = now.atOffset(ZoneOffset.UTC)
                it[revokedAt] = now.atOffset(ZoneOffset.UTC)
            } == 1
        }

    override suspend fun accept(
        invitation: OrganizationInvitation,
        userId: UUID,
        membershipId: UUID,
        now: Instant,
    ): Boolean =
        mapMembershipConflict {
            query {
                val accepted =
                    InvitationsTable.update({
                        (InvitationsTable.id eq invitation.id) and
                            (InvitationsTable.status eq PENDING_STATUS) and
                            (InvitationsTable.expiresAt greater now.atOffset(ZoneOffset.UTC))
                    }) {
                        it[status] = "accepted"
                        it[acceptedBy] = userId
                        it[acceptedAt] = now.atOffset(ZoneOffset.UTC)
                        it[updatedAt] = now.atOffset(ZoneOffset.UTC)
                    } == 1
                if (!accepted) return@query false

                MembershipsTable.insert {
                    it[id] = membershipId
                    it[organizationId] = invitation.organizationId
                    it[MembershipsTable.userId] = userId
                    it[role] = "member"
                    it[roleId] = invitation.role.id
                    it[status] = "active"
                    it[joinedAt] = now.atOffset(ZoneOffset.UTC)
                }
                true
            }
        }
}

private suspend fun <T> mapInvitationConflict(block: suspend () -> T): T =
    try {
        block()
    } catch (exception: Exception) {
        if (exception.hasSqlState("23505")) throw InvitationAlreadyExistsException()
        throw exception
    }

private suspend fun <T> mapMembershipConflict(block: suspend () -> T): T =
    try {
        block()
    } catch (exception: Exception) {
        if (exception.hasConstraint("organization_memberships_one_active_per_user")) {
            throw ActiveMembershipAlreadyExistsException()
        }
        throw exception
    }

private fun Throwable.hasSqlState(sqlState: String): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == sqlState }

private fun Throwable.hasConstraint(constraint: String): Boolean =
    generateSequence(this) { it.cause }.any { it.message?.contains(constraint) == true }

private const val PENDING_STATUS = "pending"

private object OrganizationsTable : Table("organizations") {
    val id = javaUUID("id")
    val name = varchar("name", 160)
}

private object UsersTable : Table("users") {
    val id = javaUUID("id")
}

private object RolesTable : Table("roles") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(OrganizationsTable.id)
    val key = varchar("key", 80)
    val name = varchar("name", 120)
    val description = text("description").nullable()
    val isSystem = bool("is_system")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

private object InvitationsTable : Table("organization_invitations") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(OrganizationsTable.id)
    val email = varchar("email", 320)
    val roleId = javaUUID("role_id").references(RolesTable.id)
    val invitedBy = javaUUID("invited_by").references(UsersTable.id)
    val tokenHash = char("token_hash", 64)
    val status = varchar("status", 32)
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val acceptedBy = javaUUID("accepted_by").nullable()
    val acceptedAt = timestampWithTimeZone("accepted_at").nullable()
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
}

private object MembershipsTable : Table("organization_memberships") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(OrganizationsTable.id)
    val userId = javaUUID("user_id").references(UsersTable.id)
    val role = varchar("role", 32)
    val roleId = javaUUID("role_id").references(RolesTable.id)
    val status = varchar("status", 32)
    val joinedAt = timestampWithTimeZone("joined_at")
}

private fun invitationQuery() =
    (InvitationsTable innerJoin RolesTable)
        .join(
            OrganizationsTable,
            JoinType.INNER,
            onColumn = InvitationsTable.organizationId,
            otherColumn = OrganizationsTable.id,
        )
        .selectAll()

private fun org.jetbrains.exposed.v1.core.ResultRow.toInvitation(now: Instant): OrganizationInvitation {
    val persistedStatus = InvitationStatus.valueOf(this[InvitationsTable.status].uppercase())
    val expiresAt = this[InvitationsTable.expiresAt].toInstant()
    val effectiveStatus =
        if (persistedStatus == InvitationStatus.PENDING && !expiresAt.isAfter(now)) InvitationStatus.EXPIRED else persistedStatus
    return OrganizationInvitation(
        id = this[InvitationsTable.id],
        organizationId = this[InvitationsTable.organizationId],
        organizationName = this[OrganizationsTable.name],
        email = this[InvitationsTable.email],
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
        invitedBy = this[InvitationsTable.invitedBy],
        tokenHash = this[InvitationsTable.tokenHash],
        status = effectiveStatus,
        expiresAt = expiresAt,
        createdAt = this[InvitationsTable.createdAt].toInstant(),
        updatedAt = this[InvitationsTable.updatedAt].toInstant(),
        acceptedBy = this[InvitationsTable.acceptedBy],
        acceptedAt = this[InvitationsTable.acceptedAt]?.toInstant(),
        revokedAt = this[InvitationsTable.revokedAt]?.toInstant(),
    )
}
