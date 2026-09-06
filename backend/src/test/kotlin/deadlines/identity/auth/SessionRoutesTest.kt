package deadlines.identity.auth

import deadlines.application.module
import deadlines.config.AuthConfig
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionRoutesTest {
    private val now = Instant.now()
    private val tokenService =
        TokenService(
            AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600),
            Clock.fixed(now, ZoneOffset.UTC),
        )

    @Test
    fun `requires authentication to list sessions`() =
        testApplication {
            application { module(tokenService = tokenService, sessionService = SessionService(MemorySessionRepository())) }

            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/sessions").status)
        }

    @Test
    fun `lists and revokes only the authenticated users sessions`() =
        testApplication {
            val userId = UUID.randomUUID()
            val issued = tokenService.issue(userId)
            val otherSessionId = UUID.randomUUID()
            val repository = MemorySessionRepository()
            repository.create(session(issued.sessionId, userId, "current"))
            repository.create(session(otherSessionId, userId, "other"))
            val foreignSessionId = UUID.randomUUID()
            val foreignUserId = UUID.randomUUID()
            repository.create(session(foreignSessionId, foreignUserId, "foreign"))
            application {
                module(
                    tokenService = tokenService,
                    sessionService = SessionService(repository, Clock.fixed(now, ZoneOffset.UTC)),
                )
            }

            val listed =
                client.get("/api/v1/sessions") {
                    bearerAuth(issued.accessToken)
                }
            assertEquals(HttpStatusCode.OK, listed.status)
            val sessions = Json.parseToJsonElement(listed.bodyAsText()).jsonObject.getValue("data").jsonArray
            assertEquals(2, sessions.size)
            assertTrue(
                sessions.any { item ->
                    val session = item.jsonObject
                    session.getValue("id").jsonPrimitive.content == issued.sessionId.toString() &&
                        session.getValue("isCurrent").jsonPrimitive.content == "true"
                },
            )

            val foreign =
                client.delete("/api/v1/sessions/$foreignSessionId") {
                    bearerAuth(issued.accessToken)
                }
            assertEquals(HttpStatusCode.NotFound, foreign.status)

            val revoked =
                client.delete("/api/v1/sessions/$otherSessionId") {
                    bearerAuth(issued.accessToken)
                }
            assertEquals(HttpStatusCode.NoContent, revoked.status)

            val revokedAll =
                client.post("/api/v1/sessions/revoke-all") {
                    bearerAuth(issued.accessToken)
                }
            assertEquals(HttpStatusCode.NoContent, revokedAll.status)
            assertEquals(0, repository.listActive(userId, now).size)
            assertEquals(1, repository.listActive(foreignUserId, now).size)
        }

    private fun session(id: UUID, userId: UUID, suffix: String) =
        Session(
            id = id,
            userId = userId,
            refreshTokenHash = suffix.padEnd(64, '0'),
            userAgent = "Test browser $suffix",
            ipAddress = "127.0.0.1",
            expiresAt = now.plusSeconds(3600),
            createdAt = now,
        )
}

private class MemorySessionRepository : SessionRepository {
    private val sessions = mutableListOf<Session>()
    private val revoked = mutableSetOf<UUID>()

    override suspend fun create(session: Session) {
        sessions += session
    }

    override suspend fun findActive(refreshTokenHash: String, now: Instant): Session? =
        sessions.firstOrNull { it.refreshTokenHash == refreshTokenHash && it.id !in revoked && it.expiresAt > now }

    override suspend fun rotate(currentHash: String, replacement: Session, now: Instant): Boolean {
        val current = findActive(currentHash, now) ?: return false
        revoked += current.id
        sessions += replacement
        return true
    }

    override suspend fun revoke(refreshTokenHash: String, now: Instant): Boolean {
        val session = findActive(refreshTokenHash, now) ?: return false
        return revoked.add(session.id)
    }

    override suspend fun revokeAll(userId: UUID, now: Instant): Int =
        sessions.filter { it.userId == userId && it.id !in revoked }.count { revoked.add(it.id) }

    override suspend fun listActive(userId: UUID, now: Instant): List<Session> =
        sessions.filter { it.userId == userId && it.id !in revoked && it.expiresAt > now }

    override suspend fun revoke(userId: UUID, sessionId: UUID, now: Instant): Boolean {
        val session = sessions.firstOrNull { it.id == sessionId && it.userId == userId && it.id !in revoked }
            ?: return false
        return revoked.add(session.id)
    }
}
