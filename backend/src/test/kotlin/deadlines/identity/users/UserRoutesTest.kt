package deadlines.identity.users

import deadlines.application.module
import deadlines.config.AuthConfig
import deadlines.identity.auth.TokenService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `supports the complete local user lifecycle`() =
        testApplication {
            application { module(UserService(InMemoryUserRepository()), tokenService = tokenService) }

            val created =
                client.post("/api/v1/users") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"email":"Tarik@Example.com","firstName":"Tarik","lastName":"Villalobos"}""",
                    )
                }

            assertEquals(HttpStatusCode.Created, created.status)
            val id = Json.parseToJsonElement(created.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content
            assertEquals("/api/v1/users/$id", created.headers[HttpHeaders.Location])

            val found = client.get("/api/v1/users/$id")
            assertEquals(HttpStatusCode.OK, found.status)

            val listed = client.get("/api/v1/users?page=1&limit=20")
            assertEquals(HttpStatusCode.OK, listed.status)
            assertEquals(
                "1",
                Json.parseToJsonElement(listed.bodyAsText())
                    .jsonObject.getValue("pagination")
                    .jsonObject.getValue("total")
                    .jsonPrimitive.content,
            )

            val updated =
                client.patch("/api/v1/users/$id") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"firstName":"T.","status":"disabled"}""")
                }
            assertEquals(HttpStatusCode.OK, updated.status)
            assertEquals(
                "disabled",
                Json.parseToJsonElement(updated.bodyAsText()).jsonObject.getValue("status").jsonPrimitive.content,
            )

            val deleted = client.delete("/api/v1/users/$id")
            assertEquals(HttpStatusCode.NoContent, deleted.status)
        }

    @Test
    fun `rejects malformed identifiers`() =
        testApplication {
            application { module(UserService(InMemoryUserRepository()), tokenService = tokenService) }

            val response = client.get("/api/v1/users/not-a-uuid")

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertEquals(
                "{\"error\":{\"code\":\"VALIDATION_ERROR\",\"message\":\"Invalid user data\"," +
                    "\"details\":{\"id\":\"must be a valid UUID\"}}}",
                response.bodyAsText(),
            )
        }

    @Test
    fun `rejects malformed json`() =
        testApplication {
            application { module(UserService(InMemoryUserRepository()), tokenService = tokenService) }

            val response =
                client.post("/api/v1/users") {
                    contentType(ContentType.Application.Json)
                    setBody("{")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
