package opensources.organizations.access

import opensources.organizations.OrganizationAccessDeniedException
import opensources.organizations.authorization.PlatformPermission
import opensources.organizations.authorization.testAuthorization
import java.time.Clock
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PermissionServiceTest {
    private val userId = UUID.randomUUID()
    private val organizationId = UUID.randomUUID()
    private val systemPermission = permission(null, true, "roles.read")
    private val customPermission = permission(organizationId, false, "opensources.manage")

    @Test
    fun `lists system and organization permissions without leaking another organization`() =
        runTest {
            val foreign = permission(UUID.randomUUID(), false, "foreign.manage")
            val service = service(listOf(systemPermission, customPermission, foreign))

            assertEquals(
                setOf("roles.read", "opensources.manage"),
                service.list(userId).data.map { it.key }.toSet(),
            )
            assertFailsWith<PermissionNotFoundException> { service.get(userId, foreign.id) }
        }

    @Test
    fun `authorized user creates updates and deletes a custom permission`() =
        runTest {
            val repository = MemoryPermissionRepository()
            val id = UUID.randomUUID()
            val service = service(repository = repository, idGenerator = { id })

            val created = service.create(userId, CreatePermissionRequest(" Records.Manage ", " Manage records ", "Details"))
            assertEquals("records.manage", created.key)
            assertEquals(id.toString(), created.id)

            val updated = service.update(userId, id, UpdatePermissionRequest(name = "Updated permission"))
            assertEquals("Updated permission", updated.name)
            assertEquals(null, service.update(userId, id, UpdatePermissionRequest(description = "")).description)
            service.delete(userId, id)
            assertEquals(emptyList(), repository.values)
        }

    @Test
    fun `system permissions are immutable and mutations require permission`() =
        runTest {
            val repository = MemoryPermissionRepository(listOf(systemPermission))
            val ownerService = service(repository = repository)
            assertFailsWith<SystemPermissionImmutableException> {
                ownerService.update(userId, systemPermission.id, UpdatePermissionRequest(name = "Changed"))
            }
            assertFailsWith<SystemPermissionImmutableException> {
                ownerService.delete(userId, systemPermission.id)
            }

            val memberService = service(repository = repository, granted = emptySet())
            assertFailsWith<OrganizationAccessDeniedException> {
                memberService.create(userId, CreatePermissionRequest("custom.read", "Custom read"))
            }
        }

    @Test
    fun `validates permission fields and empty updates`() =
        runTest {
            val service = service(listOf(customPermission))
            assertFailsWith<AccessValidationException> {
                service.create(userId, CreatePermissionRequest("not valid", "Valid name"))
            }
            assertFailsWith<AccessValidationException> {
                service.update(userId, customPermission.id, UpdatePermissionRequest())
            }
        }

    private fun service(
        initial: List<Permission> = emptyList(),
        repository: MemoryPermissionRepository = MemoryPermissionRepository(initial),
        granted: Set<String> = setOf(
            PlatformPermission.PERMISSIONS_READ,
            PlatformPermission.PERMISSIONS_CREATE,
            PlatformPermission.PERMISSIONS_UPDATE,
            PlatformPermission.PERMISSIONS_DELETE,
        ),
        idGenerator: () -> UUID = UUID::randomUUID,
    ) = PermissionService(
        testAuthorization(userId, organizationId, *granted.toTypedArray()),
        repository,
        Clock.fixed(accessTestNow, ZoneOffset.UTC),
        idGenerator,
    )

    private fun permission(organizationId: UUID?, isSystem: Boolean, key: String) =
        Permission(
            UUID.randomUUID(),
            organizationId,
            key,
            key,
            null,
            isSystem,
            accessTestNow,
            accessTestNow,
        )
}
