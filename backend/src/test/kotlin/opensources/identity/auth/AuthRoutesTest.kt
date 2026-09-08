package opensources.identity.auth

import opensources.application.module
import opensources.config.AbuseProtectionConfig
import opensources.config.AuthConfig
import opensources.identity.users.UserProfileResponse
import opensources.identity.users.UserResponse
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthRoutesTest {
    private val tokenService =
        TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `register and login routes expose authentication responses`() =
        testApplication {
            application { module(authService = FakeAuthOperations(), tokenService = tokenService) }

            val register =
                client.post("/api/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"user@example.com","password":"password-123","firstName":"User","lastName":"Name"}""")
                }
            val login =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    header("X-Device-Id", UUID.randomUUID().toString())
                    setBody("""{"email":"user@example.com","password":"password-123"}""")
                }

            assertEquals(HttpStatusCode.Created, register.status)
            assertEquals(HttpStatusCode.OK, login.status)
        }

    @Test
    fun `me requires a valid access token`() =
        testApplication {
            val auth = FakeAuthOperations()
            application { module(authService = auth, tokenService = tokenService) }

            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/auth/me").status)

            val authenticated =
                client.get("/api/v1/auth/me") {
                    bearerAuth(tokenService.issue(auth.userId).accessToken)
                }
            assertEquals(HttpStatusCode.OK, authenticated.status)
        }

    @Test
    fun `change password requires a valid access token`() =
        testApplication {
            val auth = FakeAuthOperations()
            application { module(authService = auth, tokenService = tokenService) }

            val response =
                client.patch("/api/v1/auth/password") {
                    bearerAuth(tokenService.issue(auth.userId).accessToken)
                    contentType(ContentType.Application.Json)
                    header("X-Device-Id", UUID.randomUUID().toString())
                    setBody(
                        """{"currentPassword":"password-123","newPassword":"new-password-123","refreshToken":"refresh"}""",
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("new-password-123", auth.changedPassword?.newPassword)
        }

    @Test
    fun `limits repeated failed login attempts`() =
        testApplication {
            val protection =
                AuthenticationAbuseProtection(
                    AbuseProtectionConfig(
                        rateLimitWindowSeconds = 60,
                        rateLimitMaxRequests = 10,
                        loginFailureThreshold = 1,
                        loginLockoutBaseSeconds = 60,
                        loginLockoutMaxSeconds = 60,
                    ),
                )
            application { module(authService = FailingAuthOperations(), tokenService = tokenService, abuseProtection = protection) }

            repeat(2) { attempt ->
                val response =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        header("X-Device-Id", UUID.randomUUID().toString())
                        setBody("""{"email":"user@example.com","password":"wrong-password"}""")
                    }
                assertEquals(if (attempt == 0) HttpStatusCode.Unauthorized else HttpStatusCode.TooManyRequests, response.status)
            }
        }
}

private class FakeAuthOperations : AuthOperations {
    val userId: UUID = UUID.randomUUID()
    private val user =
        UserResponse(
            userId.toString(),
            "user@example.com",
            "active",
            UserProfileResponse("User", "Name"),
            "2026-09-05T12:00:00Z",
            "2026-09-05T12:00:00Z",
        )
    private val response = AuthResponse("access", "refresh", expiresIn = 900, user = user)
    var changedPassword: ChangePasswordRequest? = null

    override suspend fun register(request: RegisterRequest, context: SessionContext) = RegistrationResponse(user)
    override suspend fun login(request: LoginRequest, context: SessionContext) = response
    override suspend fun reactivate(request: ReactivateAccountRequest, context: SessionContext) = response
    override suspend fun refresh(refreshToken: String, context: SessionContext) = response
    override suspend fun logout(refreshToken: String) = Unit
    override suspend fun me(userId: UUID) = user
    override suspend fun changePassword(userId: UUID, request: ChangePasswordRequest, context: SessionContext): AuthResponse {
        changedPassword = request
        return response
    }
}

private class FailingAuthOperations : AuthOperations {
    override suspend fun register(request: RegisterRequest, context: SessionContext): RegistrationResponse = error("not used")

    override suspend fun login(request: LoginRequest, context: SessionContext): AuthResponse = throw InvalidCredentialsException()

    override suspend fun reactivate(request: ReactivateAccountRequest, context: SessionContext): AuthResponse = error("not used")

    override suspend fun refresh(refreshToken: String, context: SessionContext): AuthResponse = error("not used")

    override suspend fun logout(refreshToken: String) = Unit

    override suspend fun me(userId: UUID): opensources.identity.users.UserResponse = error("not used")

    override suspend fun changePassword(userId: UUID, request: ChangePasswordRequest, context: SessionContext): AuthResponse = error("not used")
}
