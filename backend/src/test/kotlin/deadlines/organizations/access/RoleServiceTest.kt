package deadlines.organizations.access

import deadlines.organizations.MembershipRole
import deadlines.organizations.OrganizationAccessDeniedException
import java.time.Clock
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoleServiceTest {
    private val userId = UUID.randomUUID()
    private val organizationId = UUID.randomUUID()
    private val ownerRole = role("owner", true)
    private val memberRole = role("member", true)
    private val customRole = role("manager", false)
    private val systemPermission = permission(null, "members.read")
    private val customPermission = permission(organizationId, "deadlines.manage")

    @Test
    fun `lists and reads only roles from the current organization`() =
        runTest {
            val foreign = role("foreign", false, UUID.randomUUID())
            val service = service(listOf(ownerRole, memberRole, foreign))

            assertEquals(setOf("owner", "member"), service.list(userId).data.map { it.key }.toSet())
            assertEquals("owner", service.get(userId, ownerRole.id).key)
            assertFailsWith<RoleNotFoundException> { service.get(userId, foreign.id) }
        }

    @Test
    fun `owner creates updates and deletes a custom role`() =
        runTest {
            val repository = MemoryRoleRepository()
            val roleId = UUID.randomUUID()
            val service = service(repository = repository, idGenerator = { roleId })

            val created = service.create(userId, CreateRoleRequest(" Project-Manager ", " Project Manager ", "Details"))
            assertEquals("project-manager", created.key)
            assertEquals(roleId.toString(), created.id)

            val updated = service.update(userId, roleId, UpdateRoleRequest(name = "Delivery Manager"))
            assertEquals("Delivery Manager", updated.name)
            assertEquals(null, service.update(userId, roleId, UpdateRoleRequest(description = "")).description)
            service.delete(userId, roleId)
            assertEquals(emptyList(), repository.values)
        }

    @Test
    fun `system roles are immutable and members cannot create roles`() =
        runTest {
            val repository = MemoryRoleRepository(listOf(ownerRole, memberRole))
            val ownerService = service(repository = repository)
            assertFailsWith<SystemRoleImmutableException> {
                ownerService.update(userId, memberRole.id, UpdateRoleRequest(name = "Changed"))
            }
            assertFailsWith<SystemRoleImmutableException> { ownerService.delete(userId, ownerRole.id) }

            val memberService = service(repository = repository, membershipRole = MembershipRole.MEMBER)
            assertFailsWith<OrganizationAccessDeniedException> {
                memberService.create(userId, CreateRoleRequest("new-role", "New role"))
            }
        }

    @Test
    fun `replaces permissions only with permissions available to the organization`() =
        runTest {
            val roleRepository = MemoryRoleRepository(listOf(ownerRole, memberRole, customRole))
            roleRepository.availablePermissions = listOf(systemPermission, customPermission)
            val service = service(
                repository = roleRepository,
                permissions = MemoryPermissionRepository(roleRepository.availablePermissions),
            )

            val response =
                service.replacePermissions(
                    userId,
                    customRole.id,
                    ReplaceRolePermissionsRequest(listOf(systemPermission.id.toString(), customPermission.id.toString())),
                )
            assertEquals(setOf("members.read", "deadlines.manage"), response.data.map { it.key }.toSet())

            assertFailsWith<OwnerPermissionsImmutableException> {
                service.replacePermissions(userId, ownerRole.id, ReplaceRolePermissionsRequest(emptyList()))
            }
            assertFailsWith<AccessValidationException> {
                service.replacePermissions(
                    userId,
                    customRole.id,
                    ReplaceRolePermissionsRequest(listOf(UUID.randomUUID().toString())),
                )
            }
        }

    private fun service(
        initial: List<Role> = emptyList(),
        repository: MemoryRoleRepository = MemoryRoleRepository(initial),
        permissions: MemoryPermissionRepository = MemoryPermissionRepository(listOf(systemPermission, customPermission)),
        membershipRole: MembershipRole = MembershipRole.OWNER,
        idGenerator: () -> UUID = UUID::randomUUID,
    ) = RoleService(
        TestOrganizationRepository(accessContext(userId, organizationId, membershipRole)),
        repository,
        permissions,
        Clock.fixed(accessTestNow, ZoneOffset.UTC),
        idGenerator,
    )

    private fun role(key: String, isSystem: Boolean, orgId: UUID = organizationId) =
        Role(UUID.randomUUID(), orgId, key, key, null, isSystem, accessTestNow, accessTestNow)

    private fun permission(orgId: UUID?, key: String) =
        Permission(UUID.randomUUID(), orgId, key, key, null, orgId == null, accessTestNow, accessTestNow)
}
