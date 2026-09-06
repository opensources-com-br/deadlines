package deadlines.organizations.access

import deadlines.shared.database.DatabaseQuery
import java.sql.SQLException
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

interface PermissionRepository {
    suspend fun list(organizationId: UUID): List<Permission>

    suspend fun findById(organizationId: UUID, permissionId: UUID): Permission?

    suspend fun create(permission: Permission): Permission

    suspend fun update(permission: Permission): Permission

    suspend fun delete(organizationId: UUID, permissionId: UUID): Boolean
}

class ExposedPermissionRepository(
    private val query: DatabaseQuery,
) : PermissionRepository {
    override suspend fun list(organizationId: UUID): List<Permission> =
        query {
            PermissionsTable.selectAll()
                .where {
                    PermissionsTable.organizationId.isNull() or
                        (PermissionsTable.organizationId eq organizationId)
                }
                .orderBy(PermissionsTable.isSystem to SortOrder.DESC, PermissionsTable.key to SortOrder.ASC)
                .map { it.toPermission() }
        }

    override suspend fun findById(organizationId: UUID, permissionId: UUID): Permission? =
        query {
            PermissionsTable.selectAll()
                .where {
                    (PermissionsTable.id eq permissionId) and
                        (PermissionsTable.organizationId.isNull() or (PermissionsTable.organizationId eq organizationId))
                }
                .singleOrNull()
                ?.toPermission()
        }

    override suspend fun create(permission: Permission): Permission =
        mapPermissionConflict {
            query {
                PermissionsTable.insert {
                    it[id] = permission.id
                    it[organizationId] = permission.organizationId
                    it[key] = permission.key
                    it[name] = permission.name
                    it[description] = permission.description
                    it[isSystem] = permission.isSystem
                    it[createdAt] = permission.createdAt.atOffset(ZoneOffset.UTC)
                    it[updatedAt] = permission.updatedAt.atOffset(ZoneOffset.UTC)
                }
                permission
            }
        }

    override suspend fun update(permission: Permission): Permission =
        mapPermissionConflict {
            query {
                PermissionsTable.update({
                    (PermissionsTable.id eq permission.id) and
                        (PermissionsTable.organizationId eq permission.organizationId!!)
                }) {
                    it[key] = permission.key
                    it[name] = permission.name
                    it[description] = permission.description
                    it[updatedAt] = permission.updatedAt.atOffset(ZoneOffset.UTC)
                }
                permission
            }
        }

    override suspend fun delete(organizationId: UUID, permissionId: UUID): Boolean =
        query {
            PermissionsTable.deleteWhere {
                (PermissionsTable.id eq permissionId) and
                    (PermissionsTable.organizationId eq organizationId)
            } == 1
        }
}

private suspend fun <T> mapPermissionConflict(block: suspend () -> T): T =
    try {
        block()
    } catch (exception: Exception) {
        if (exception.hasSqlState("23505")) throw PermissionAlreadyExistsException()
        throw exception
    }

private fun Throwable.hasSqlState(sqlState: String): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == sqlState }

private object AccessOrganizationsTable : Table("organizations") {
    val id = javaUUID("id")
}

internal object PermissionsTable : Table("permissions") {
    val id = javaUUID("id")
    val organizationId = javaUUID("organization_id").references(AccessOrganizationsTable.id).nullable()
    val key = varchar("key", 100)
    val name = varchar("name", 120)
    val description = text("description").nullable()
    val isSystem = bool("is_system")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

private fun org.jetbrains.exposed.v1.core.ResultRow.toPermission() =
    Permission(
        id = this[PermissionsTable.id],
        organizationId = this[PermissionsTable.organizationId],
        key = this[PermissionsTable.key],
        name = this[PermissionsTable.name],
        description = this[PermissionsTable.description],
        isSystem = this[PermissionsTable.isSystem],
        createdAt = this[PermissionsTable.createdAt].toInstant(),
        updatedAt = this[PermissionsTable.updatedAt].toInstant(),
    )
