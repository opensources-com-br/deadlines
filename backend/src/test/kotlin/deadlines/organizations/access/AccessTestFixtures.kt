package deadlines.organizations.access

import deadlines.organizations.MembershipRole
import deadlines.organizations.MembershipStatus
import deadlines.organizations.Organization
import deadlines.organizations.OrganizationContext
import deadlines.organizations.OrganizationMembership
import deadlines.organizations.OrganizationRepository
import java.time.Instant
import java.util.UUID

internal val accessTestNow: Instant = Instant.parse("2026-09-06T18:00:00Z")

internal fun accessContext(
    userId: UUID,
    organizationId: UUID = UUID.randomUUID(),
    membershipRole: MembershipRole = MembershipRole.OWNER,
) = OrganizationContext(
    Organization(organizationId, "Acme", "acme", userId, accessTestNow, accessTestNow),
    OrganizationMembership(
        UUID.randomUUID(),
        organizationId,
        userId,
        membershipRole,
        MembershipStatus.ACTIVE,
        accessTestNow,
        null,
    ),
)

internal class TestOrganizationRepository(
    var context: OrganizationContext?,
) : OrganizationRepository {
    override suspend fun createWithOwner(context: OrganizationContext): OrganizationContext {
        this.context = context
        return context
    }

    override suspend fun findCurrentByUser(userId: UUID): OrganizationContext? =
        context?.takeIf { it.membership.userId == userId }

    override suspend fun update(organization: Organization): Organization = organization
}

internal class MemoryPermissionRepository(
    initial: List<Permission> = emptyList(),
) : PermissionRepository {
    val values = initial.toMutableList()

    override suspend fun list(organizationId: UUID): List<Permission> =
        values.filter { it.organizationId == null || it.organizationId == organizationId }

    override suspend fun findById(organizationId: UUID, permissionId: UUID): Permission? =
        values.firstOrNull { it.id == permissionId && (it.organizationId == null || it.organizationId == organizationId) }

    override suspend fun create(permission: Permission): Permission {
        values += permission
        return permission
    }

    override suspend fun update(permission: Permission): Permission {
        values.replaceAll { if (it.id == permission.id) permission else it }
        return permission
    }

    override suspend fun delete(organizationId: UUID, permissionId: UUID): Boolean =
        values.removeIf { it.id == permissionId && it.organizationId == organizationId }
}

internal class MemoryRoleRepository(
    initial: List<Role> = emptyList(),
) : RoleRepository {
    val values = initial.toMutableList()
    val permissionIds = mutableMapOf<UUID, List<UUID>>()
    var availablePermissions: List<Permission> = emptyList()

    override suspend fun list(organizationId: UUID): List<Role> = values.filter { it.organizationId == organizationId }

    override suspend fun findById(organizationId: UUID, roleId: UUID): Role? =
        values.firstOrNull { it.id == roleId && it.organizationId == organizationId }

    override suspend fun create(role: Role): Role {
        values += role
        return role
    }

    override suspend fun update(role: Role): Role {
        values.replaceAll { if (it.id == role.id) role else it }
        return role
    }

    override suspend fun delete(organizationId: UUID, roleId: UUID): Boolean =
        values.removeIf { it.id == roleId && it.organizationId == organizationId }

    override suspend fun listPermissions(roleId: UUID): List<Permission> {
        val ids = permissionIds[roleId].orEmpty()
        return availablePermissions.filter { it.id in ids }
    }

    override suspend fun replacePermissions(roleId: UUID, permissionIds: List<UUID>) {
        this.permissionIds[roleId] = permissionIds
    }
}
