package opensources.identity.users

import opensources.application.module
import opensources.config.AuthConfig
import opensources.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.UUID

class UserRoutesTest {
    private val tokenService =
        TokenService(AuthConfig("a-local-test-secret-with-32-characters", "issuer", "audience", 900, 3600))

    @Test
    fun `authenticated user can read and update own profile`() =
        testApplication {
            val service = UserService(InMemoryUserRepository())
            val user =
                service.create(
                    CreateUserRequest(
                        email = "user@example.com",
                        firstName = "User",
                        lastName = "Name",
                    ),
                )
            application { module(userService = service, tokenService = tokenService) }

            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/users/me").status)

            val found =
                client.get("/api/v1/users/me") {
                    bearerAuth(tokenService.issue(user.id).accessToken)
                }
            assertEquals(HttpStatusCode.OK, found.status)

            val updated =
                client.patch("/api/v1/users/me") {
                    bearerAuth(tokenService.issue(user.id).accessToken)
                    contentType(ContentType.Application.Json)
                    setBody("""{"firstName":"Updated","lastName":"User"}""")
                }
            assertEquals(HttpStatusCode.OK, updated.status)
            val profile = Json.parseToJsonElement(updated.bodyAsText()).jsonObject.getValue("profile").jsonObject
            assertEquals("Updated", profile.getValue("firstName").jsonPrimitive.content)
            assertEquals("User", profile.getValue("lastName").jsonPrimitive.content)
        }

    @Test
    fun `disabled accounts cannot use previously issued access tokens`() =
        testApplication {
            val repository = InMemoryUserRepository()
            val service = UserService(repository)
            val user = service.create(CreateUserRequest("user@example.com", "User", "Name"))
            service.disable(user.id)
            application { module(userService = service, userRepository = repository, tokenService = tokenService) }

            assertEquals(
                HttpStatusCode.Unauthorized,
                client.get("/api/v1/users/me") { bearerAuth(tokenService.issue(user.id).accessToken) }.status,
            )
        }

    @Test
    fun `deleted accounts cannot use previously issued access tokens`() =
        testApplication {
            val repository = InMemoryUserRepository()
            val service = UserService(repository)
            val user = service.create(CreateUserRequest("user@example.com", "User", "Name"))
            repository.update(user.copy(status = UserStatus.DELETED, disabledAt = user.createdAt, deletedAt = user.createdAt))
            application { module(userService = service, userRepository = repository, tokenService = tokenService) }

            assertEquals(
                HttpStatusCode.Unauthorized,
                client.get("/api/v1/users/me") { bearerAuth(tokenService.issue(user.id).accessToken) }.status,
            )
        }

    @Test
    fun `technical user management endpoints are not public`() =
        testApplication {
            application { module(UserService(InMemoryUserRepository()), tokenService = tokenService) }

            assertEquals(HttpStatusCode.NotFound, client.post("/api/v1/users").status)
            assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/users").status)
            assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/users/${UUID.randomUUID()}").status)
            assertEquals(HttpStatusCode.NotFound, client.patch("/api/v1/users/${UUID.randomUUID()}").status)
        }

    @Test
    fun `account lifecycle endpoints require authentication and forward password`() =
        testApplication {
            val lifecycle = RecordingAccountLifecycle()
            application {
                module(
                    userService = UserService(InMemoryUserRepository()),
                    tokenService = tokenService,
                    accountLifecycleService = lifecycle,
                )
            }
            val userId = UUID.randomUUID()

            assertEquals(
                HttpStatusCode.Unauthorized,
                client.post("/api/v1/users/me/deactivate") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"password":"secret"}""")
                }.status,
            )

            val deactivated = client.post("/api/v1/users/me/deactivate") {
                bearerAuth(tokenService.issue(userId).accessToken)
                contentType(ContentType.Application.Json)
                setBody("""{"password":"secret"}""")
            }
            assertEquals(HttpStatusCode.NoContent, deactivated.status)
            assertEquals(userId to "secret", lifecycle.deactivated)

            val deleted = client.delete("/api/v1/users/me") {
                bearerAuth(tokenService.issue(userId).accessToken)
                contentType(ContentType.Application.Json)
                setBody("""{"password":"secret"}""")
            }
            assertEquals(HttpStatusCode.NoContent, deleted.status)
            assertEquals(userId to "secret", lifecycle.deleted)
        }
}

private class RecordingAccountLifecycle : AccountLifecycleOperations {
    var deactivated: Pair<UUID, String>? = null
    var deleted: Pair<UUID, String>? = null

    override suspend fun deactivate(userId: UUID, password: String) {
        deactivated = userId to password
    }

    override suspend fun delete(userId: UUID, password: String) {
        deleted = userId to password
    }
}
