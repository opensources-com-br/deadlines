package opensources.application

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import opensources.config.HttpConfig

class CorsTest {
    @Test
    fun `allows configured web origin`() = testApplication {
        application { module(http = HttpConfig(8080, listOf("https://app.example.com"))) }

        val response = client.get("/health") { header(HttpHeaders.Origin, "https://app.example.com") }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("https://app.example.com", response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun `does not reflect unconfigured origins`() = testApplication {
        application { module(http = HttpConfig(8080, listOf("https://app.example.com"))) }

        val response = client.get("/health") { header(HttpHeaders.Origin, "https://untrusted.example.com") }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(null, response.headers[HttpHeaders.AccessControlAllowOrigin])
    }
}
