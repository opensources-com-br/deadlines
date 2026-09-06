package deadlines.organizations.access

import deadlines.shared.database.DatabaseQuery
import java.sql.SQLException
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

interface RoleRepository {
    suspend fun list(organizationId: UUID): List<Role>

    suspend fun findById(organizationId: UUID, roleId: UUID): Role?

    suspend fun create(role: Role): Role

    suspend fun update(role: Role): Role

    suspend fun delete(organizationId: UUID, roleId: UUID): Boolean

    suspend fun listPermissions(roleId: UUID): List<Permission>

    suspend fun replacePermissions(roleId: UUID, permissionIds: List<UUID>)
}

class ExposedRoleRepository(
    private val query: DatabaseQuery,
) : RoleRepository {
    override suspend fun list(organizationId: UUID): List<Role> =
        query {
            RolesTable.selectAll()
                .where { RolesTable.organizationId eq organizationId }
                .orderBy(RolesTable.isSystem to SortOrder.DESC, RolesTable.name to SortOrder.ASC)
                .map { it.toRole() }
        }

    override suspend fun findById(organizationId: UUID, roleId: UUID): Role? =
        query {
            RolesTable.selectAll()
                .where { (RolesTable.id eq roleId) and (RolesTable.organizationId eq organizationId) }
                .singleOrNull()
                ?.toRole()
        }

    override suspend fun create(role: Role): Role =
        mapRoleConflict {
            query {
                RolesTable.insert {
                    it[id] = role.id
                    it[organizationId] = role.organizationId
                    it[key] = role.key
                    it[name] = role.name
                    it[description] = role.description
                    it[isSystem] = role.isSystem
                    it[createdAt] = role.createdAt.atOffset(ZoneOffset.UTC)
                    it[updatedAt] = role.updatedAt.atOffset(ZoneOffset.UTC)
                }
                role
            }
        }

    override suspend fun update(role: Role): Role =
        mapRoleConflict {
            query {
                RolesTable.update({
                    (RolesTable.id eq role.id) and (RolesTable.organizationId eq role.organizationId)
                }) {
                    it[key] = role.key
                    it[name] = role.name
                    it[description] = role.description
                    it[updatedAt] = role.updatedAt.atOffset(ZoneOffset.UTC)
                }
                role
            }
        }

    override suspend fun delete(organizationId: UUID, roleId: UUID): Boolean =
        mapRoleConflict {
            query {
                RolesTable.deleteWhere {
                    (RolesTable.id eq roleId) and (RolesTable.organizationId eq organizationId)
                } == 1
            }
        }

    override suspend fun listPermissions(roleId: UUID): List<Permission> =
        query {
            (RolePermissionsTable innerJoin PermissionsTable)
                .selectAll()
                .where { RolePermissionsTable.roleId eq roleId }
                .orderBy(PermissionsTable.key to SortOrder.ASC)
                .map { row ->
                    Permission(
                        id = row[PermissionsTable.id],
                        organizationId = row[PermissionsTable.organizationId],
                        key = row[PermissionsTable.key],
                        name = row[PermissionsTable.name],
                        description = row[PermissionsTable.description],
                        isSystem = row[PermissionsTable.isSystem],
                        createdAt = row[PermissionsTable.createdAt].toInstant(),
                        updatedAt = row[PermissionsTable.updatedAt].toInstant(),
                    )
                }
        }

    override suspend fun replacePermissions(roleId: UUID, permissionIds: List<UUID>) {
        query {
            RolePermissionsTable.deleteWhere { RolePermissionsTable.roleId eq roleId }
            RolePermissionsTable.batchInsert(permissionIds.distinct()) { permissionId ->
                this[RolePermissionsTable.roleId] = roleId
                this[RolePermissionsTable.permissionId] = permissionId
            }
        }
    }
}

private suspend fun <T> mapRoleConflict(block: suspend () -> T): T =
    try {
        block()
    } catch (exception: Exception) {
        if (exception.hasRoleSqlState("23505")) throw RoleAlreadyExistsException()
        if (exception.hasRoleSqlState("23503")) throw RoleInUseException()
        throw exception
    }

private fun Throwable.hasRoleSqlState(sqlState: String): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == sqlState }

private object RoleOrganizationsTable : Table("organizations") {
    val id = javaUUID("id")
}

private object RolesTable : Table("roles") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(RoleOrganizationsTable.id)
    val key = varchar("key", 80)
    val name = varchar("name", 120)
    val description = text("description").nullable()
    val isSystem = bool("is_system")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

private object RolePermissionsTable : Table("role_permissions") {
    val roleId = javaUUID("role_id").references(RolesTable.id)
    val permissionId = javaUUID("permission_id").references(PermissionsTable.id)

    override val primaryKey = PrimaryKey(roleId, permissionId)
}

private fun org.jetbrains.exposed.v1.core.ResultRow.toRole() =
    Role(
        id = this[RolesTable.id],
        organizationId = this[RolesTable.organizationId],
        key = this[RolesTable.key],
        name = this[RolesTable.name],
        description = this[RolesTable.description],
        isSystem = this[RolesTable.isSystem],
        createdAt = this[RolesTable.createdAt].toInstant(),
        updatedAt = this[RolesTable.updatedAt].toInstant(),
    )
