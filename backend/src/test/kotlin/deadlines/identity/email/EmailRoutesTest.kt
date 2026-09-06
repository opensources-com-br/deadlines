package deadlines.identity.email

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EmailRoutesTest {
    private val tokenService = TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `public email token endpoints return no content`() = testApplication {
        val verification = FakeVerificationOperations()
        val reset = FakePasswordResetOperations()
        application { module(tokenService = tokenService, emailVerification = verification, passwordReset = reset) }

        val verify = client.post("/api/v1/auth/email/verify") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"verification-token"}""")
        }
        val forgot = client.post("/api/v1/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com"}""")
        }
        val resetResponse = client.post("/api/v1/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"reset-token","password":"new-password-123"}""")
        }

        assertEquals(HttpStatusCode.NoContent, verify.status)
        assertEquals(HttpStatusCode.NoContent, forgot.status)
        assertEquals(HttpStatusCode.NoContent, resetResponse.status)
        assertEquals("verification-token", verification.verifiedToken)
        assertEquals("user@example.com", reset.requestedEmail)
        assertEquals("reset-token" to "new-password-123", reset.resetRequest)
    }

    @Test
    fun `resend verification requires authentication`() = testApplication {
        val verification = FakeVerificationOperations()
        application { module(tokenService = tokenService, emailVerification = verification, passwordReset = FakePasswordResetOperations()) }

        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/auth/email/resend").status)

        val userId = UUID.randomUUID()
        val response = client.post("/api/v1/auth/email/resend") {
            bearerAuth(tokenService.issue(userId).accessToken)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(userId, verification.resentUserId)
    }
}

private class FakeVerificationOperations : EmailVerificationOperations {
    var verifiedToken: String? = null
    var resentUserId: UUID? = null

    override suspend fun resend(userId: UUID): Boolean {
        resentUserId = userId
        return true
    }

    override suspend fun verify(rawToken: String) {
        verifiedToken = rawToken
    }
}

private class FakePasswordResetOperations : PasswordResetOperations {
    var requestedEmail: String? = null
    var resetRequest: Pair<String, String>? = null

    override suspend fun request(emailAddress: String) {
        requestedEmail = emailAddress
    }

    override suspend fun reset(rawToken: String, password: String) {
        resetRequest = rawToken to password
    }
}
