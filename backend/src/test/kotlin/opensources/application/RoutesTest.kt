package opensources.application

import opensources.shared.errors.ApiException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutesTest {
    @Test
    fun `health reports a running application`() =
        testApplication {
            application { module() }

            val response = client.get("/health")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("{\"status\":\"ok\"}", response.bodyAsText())
            assertTrue(response.headers["X-Request-Id"]?.isNotBlank() == true)
        }

    @Test
    fun `preserves a caller supplied request ID`() =
        testApplication {
            application { module() }

            val response = client.get("/health") { header("X-Request-Id", "request-123") }

            assertEquals("request-123", response.headers["X-Request-Id"])
        }

    @Test
    fun `readiness health check reports unavailable dependencies`() =
        testApplication {
            application { module(readinessCheck = { false }) }

            val response = client.get("/health/ready")

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertTrue(response.bodyAsText().contains("\"code\":\"NOT_READY\""))
        }

    @Test
    fun `liveness health check does not depend on external services`() =
        testApplication {
            application { module(readinessCheck = { false }) }

            assertEquals(HttpStatusCode.OK, client.get("/health/live").status)
        }

    @Test
    fun `does not register billing routes without the optional module`() =
        testApplication {
            application { module(billing = null) }

            assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/plans").status)
            assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/subscriptions/current").status)
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

            val response = client.get("/test/error") { header("X-Request-Id", "request-123") }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertEquals("request-123", response.headers["X-Request-Id"])
            assertTrue(response.bodyAsText().contains("\"fields\":{\"name\":\"must not be blank\"}"))
            assertTrue(response.bodyAsText().contains("\"requestId\":\"request-123\""))
        }

    @Test
    fun `unknown routes use the shared error contract`() =
        testApplication {
            application { module() }

            val response = client.get("/missing")

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("\"code\":\"NOT_FOUND\""))
            assertTrue(response.bodyAsText().contains("\"requestId\":\"${response.headers["X-Request-Id"]}\""))
        }
}
