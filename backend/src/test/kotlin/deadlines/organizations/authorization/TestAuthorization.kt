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
            override suspend fun findByUserId(candidate: UUID): AuthorizationContext? =
                context.takeIf { candidate == userId }
        },
    )
}
