package deadlines.organizations.authorization

import java.util.UUID

fun testAuthorization(
    userId: UUID,
    organizationId: UUID,
    vararg permissions: String,
): AuthorizationOperations {
    val context =
        AuthorizationContext(
            userId = userId,
            organizationId = organizationId,
            membershipId = UUID.randomUUID(),
            roleId = UUID.randomUUID(),
            permissions = permissions.toSet(),
        )
    return AuthorizationService(
        object : AuthorizationRepository {
            override suspend fun findByUserId(userId: UUID): AuthorizationContext? =
                context.takeIf { it.userId == userId }
        },
    )
}

fun testAuthorization(
    userId: UUID,
    organizationId: UUID,
    membershipId: UUID,
    vararg permissions: String,
): AuthorizationOperations {
    val context = AuthorizationContext(userId, organizationId, membershipId, UUID.randomUUID(), permissions.toSet())
    return AuthorizationService(
        object : AuthorizationRepository {
            override suspend fun findByUserId(userId: UUID): AuthorizationContext? =
                context.takeIf { it.userId == userId }
        },
    )
}
