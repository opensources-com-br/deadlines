package deadlines.identity.auth

import deadlines.config.AuthConfig
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TokenServiceTest {
    private val now = Instant.now()
    private val service =
        TokenService(
            config = AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    @Test
    fun `issues verifiable access and opaque refresh tokens`() {
        val userId = UUID.randomUUID()
        val tokens = service.issue(userId)
        val decoded = service.verifier().verify(tokens.accessToken)

        assertEquals(userId.toString(), decoded.subject)
        assertEquals(tokens.sessionId.toString(), decoded.getClaim("sid").asString())
        assertEquals(900, tokens.accessExpiresIn)
        assertEquals(now.plusSeconds(3600), tokens.refreshExpiresAt)
        assertEquals(64, tokens.refreshTokenHash.length)
        assertNotEquals(tokens.refreshToken, tokens.refreshTokenHash)
    }

    @Test
    fun `generates a different refresh token for every issue`() {
        val userId = UUID.randomUUID()

        assertNotEquals(service.issue(userId).refreshToken, service.issue(userId).refreshToken)
    }
}
