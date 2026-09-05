package deadlines.identity.users

import deadlines.application.module
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
    @Test
    fun `supports the complete local user lifecycle`() =
        testApplication {
            application { module(UserService(InMemoryUserRepository())) }

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
            application { module(UserService(InMemoryUserRepository())) }

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
            application { module(UserService(InMemoryUserRepository())) }

            val response =
                client.post("/api/v1/users") {
                    contentType(ContentType.Application.Json)
                    setBody("{")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
