package opensources.application

import io.ktor.server.testing.testApplication
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlin.test.Test
import kotlin.test.assertEquals
import opensources.config.HttpConfig

class ClientAddressResolverTest {
    @Test
    fun `uses forwarded address only from a trusted proxy`() = testApplication {
        application {
            configurePlugins(http = HttpConfig(8080, trustedProxyAddresses = listOf("localhost")))
            routing { get("/") { call.respondText(call.clientAddress()) } }
        }
        assertEquals("203.0.113.10", client.get("/") { header("X-Forwarded-For", "203.0.113.10, 127.0.0.1") }.bodyAsText())
    }
}
