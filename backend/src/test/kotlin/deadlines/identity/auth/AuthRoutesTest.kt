package deadlines.identity.auth

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.users.UserProfileResponse
import deadlines.identity.users.UserResponse
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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
                    setBody("""{"currentPassword":"password-123","newPassword":"new-password-123"}""")
                }

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("new-password-123", auth.changedPassword?.newPassword)
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
    override suspend fun refresh(refreshToken: String, context: SessionContext) = response
    override suspend fun logout(refreshToken: String) = Unit
    override suspend fun me(userId: UUID) = user
    override suspend fun changePassword(userId: UUID, request: ChangePasswordRequest) {
        changedPassword = request
    }
}
