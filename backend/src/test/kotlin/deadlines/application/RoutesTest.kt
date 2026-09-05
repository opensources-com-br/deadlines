package deadlines.application

import deadlines.shared.errors.ApiException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutesTest {
    @Test
    fun `health reports a running application`() =
        testApplication {
            application { module() }

            val response = client.get("/health")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("{\"status\":\"ok\"}", response.bodyAsText())
        }

    @Test
    fun `known errors use the shared error contract`() =
        testApplication {
            application {
                module()
                routing {
                    get("/test/error") {
                        throw ApiException(
                            status = 422,
                            code = "VALIDATION_ERROR",
                            message = "Invalid input",
                            details = mapOf("name" to "must not be blank"),
                        )
                    }
                }
            }

            val response = client.get("/test/error")

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertEquals(
                "{\"error\":{\"code\":\"VALIDATION_ERROR\",\"message\":\"Invalid input\"," +
                    "\"details\":{\"name\":\"must not be blank\"}}}",
                response.bodyAsText(),
            )
        }

    @Test
    fun `unknown routes use the shared error contract`() =
        testApplication {
            application { module() }

            val response = client.get("/missing")

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(
                "{\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"Resource not found\"}}",
                response.bodyAsText(),
            )
        }
}
