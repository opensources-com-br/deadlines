package deadlines.identity.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import deadlines.config.AuthConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenHash: String,
    val accessExpiresIn: Long,
    val refreshExpiresAt: Instant,
)

class TokenService(
    private val config: AuthConfig,
    private val clock: Clock = Clock.systemUTC(),
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun issue(userId: UUID): IssuedTokens {
        val now = clock.instant()
        val accessExpiresAt = now.plusSeconds(config.accessTokenExpirationSeconds)
        val refreshToken = ByteArray(32).also(secureRandom::nextBytes).toBase64Url()

        return IssuedTokens(
            accessToken =
                JWT.create()
                    .withIssuer(config.jwtIssuer)
                    .withAudience(config.jwtAudience)
                    .withSubject(userId.toString())
                    .withIssuedAt(Date.from(now))
                    .withExpiresAt(Date.from(accessExpiresAt))
                    .sign(algorithm),
            refreshToken = refreshToken,
            refreshTokenHash = hashRefreshToken(refreshToken),
            accessExpiresIn = config.accessTokenExpirationSeconds,
            refreshExpiresAt = now.plusSeconds(config.refreshTokenExpirationSeconds),
        )
    }

    fun hashRefreshToken(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun verifier() =
        JWT.require(algorithm)
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .build()

    private fun ByteArray.toBase64Url(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(this)
}
