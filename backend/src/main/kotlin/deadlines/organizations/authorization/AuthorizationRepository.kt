package deadlines.organizations.authorization

import deadlines.shared.database.DatabaseQuery
import java.util.UUID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.selectAll

interface AuthorizationRepository {
    suspend fun findByUserId(userId: UUID): AuthorizationContext?
}

class ExposedAuthorizationRepository(
    private val query: DatabaseQuery,
) : AuthorizationRepository {
    override suspend fun findByUserId(userId: UUID): AuthorizationContext? =
        query {
            val membership =
                (AuthorizationMembershipsTable innerJoin AuthorizationOrganizationsTable)
                    .selectAll()
                    .where {
                        (AuthorizationMembershipsTable.userId eq userId) and
                            (AuthorizationMembershipsTable.status eq "active") and
                            (AuthorizationOrganizationsTable.status eq "active")
                    }
                    .singleOrNull()
                    ?: return@query null

            val roleId = membership[AuthorizationMembershipsTable.roleId]
            val permissionKeys =
                (AuthorizationRolePermissionsTable innerJoin AuthorizationPermissionsTable)
                    .selectAll()
                    .where { AuthorizationRolePermissionsTable.roleId eq roleId }
                    .mapTo(mutableSetOf()) { it[AuthorizationPermissionsTable.key] }

            AuthorizationContext(
                userId = membership[AuthorizationMembershipsTable.userId],
                organizationId = membership[AuthorizationMembershipsTable.organizationId],
                membershipId = membership[AuthorizationMembershipsTable.id],
                roleId = roleId,
                permissions = permissionKeys,
            )
        }
}

private object AuthorizationOrganizationsTable : Table("organizations") {
    val id = javaUUID("id")
    val status = varchar("status", 32)
}

private object AuthorizationUsersTable : Table("users") {
    val id = javaUUID("id")
}

private object AuthorizationRolesTable : Table("roles") {
    val id = javaUUID("id")
}

private object AuthorizationPermissionsTable : Table("permissions") {
    val id = javaUUID("id")
    val key = varchar("key", 100)
}

private object AuthorizationMembershipsTable : Table("organization_memberships") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(AuthorizationOrganizationsTable.id)
    val userId = javaUUID("user_id").references(AuthorizationUsersTable.id)
    val roleId = javaUUID("role_id").references(AuthorizationRolesTable.id)
    val status = varchar("status", 32)
}

private object AuthorizationRolePermissionsTable : Table("role_permissions") {
    val roleId = javaUUID("role_id").references(AuthorizationRolesTable.id)
    val permissionId = javaUUID("permission_id").references(AuthorizationPermissionsTable.id)
}
