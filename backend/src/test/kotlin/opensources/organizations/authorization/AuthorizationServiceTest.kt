package opensources.organizations.authorization

import opensources.organizations.OrganizationAccessDeniedException
import opensources.organizations.OrganizationNotFoundException
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorizationServiceTest {
    private val userId = UUID.randomUUID()
    private val context = AuthorizationContext(
        userId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        setOf("members.read", "members.invite"),
    )
    private val service = AuthorizationService(MemoryAuthorizationRepository(context))

    @Test
    fun `resolves the complete authorization context`() = runTest {
        assertEquals(context, service.context(userId))
        assertFailsWith<OrganizationNotFoundException> { service.context(UUID.randomUUID()) }
    }

    @Test
    fun `checks and requires one permission`() = runTest {
        assertTrue(service.hasPermission(userId, "members.read"))
        assertFalse(service.hasPermission(userId, "members.remove"))
        assertEquals(context, service.requirePermission(userId, "members.invite"))
        assertFailsWith<OrganizationAccessDeniedException> {
            service.requirePermission(userId, "members.remove")
        }
    }

    @Test
    fun `requires any or all requested permissions`() = runTest {
        assertEquals(context, service.requireAnyPermission(userId, setOf("members.remove", "members.read")))
        assertEquals(context, service.requireAllPermissions(userId, setOf("members.read", "members.invite")))
        assertFailsWith<OrganizationAccessDeniedException> {
            service.requireAnyPermission(userId, emptySet())
        }
        assertFailsWith<OrganizationAccessDeniedException> {
            service.requireAllPermissions(userId, setOf("members.read", "members.remove"))
        }
    }
}

private class MemoryAuthorizationRepository(
    private val context: AuthorizationContext?,
) : AuthorizationRepository {
    override suspend fun findByUserId(userId: UUID): AuthorizationContext? =
        context?.takeIf { it.userId == userId }
}
