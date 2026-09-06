package deadlines.identity.auth

import java.time.Clock
import java.util.UUID

class SessionService(
    private val repository: SessionRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun list(userId: UUID, currentSessionId: UUID?): SessionListResponse =
        SessionListResponse(
            repository.listActive(userId, clock.instant()).map { session ->
                SessionResponse(
                    id = session.id.toString(),
                    userAgent = session.userAgent,
                    ipAddress = session.ipAddress,
                    expiresAt = session.expiresAt.toString(),
                    createdAt = session.createdAt.toString(),
                    isCurrent = session.id == currentSessionId,
                )
            },
        )

    suspend fun revoke(userId: UUID, sessionId: UUID) {
        if (!repository.revoke(userId, sessionId, clock.instant())) throw SessionNotFoundException()
    }

    suspend fun revokeAll(userId: UUID) {
        repository.revokeAll(userId, clock.instant())
    }
}
