package opensources.application

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import opensources.config.HttpConfig

class RequestBodyLimitTest {
    @Test
    fun `rejects requests exceeding the configured body limit`() = testApplication {
        application { module(http = HttpConfig(8080, maxRequestBodyBytes = 3)) }

        val response = client.post("/health") {
            header(HttpHeaders.ContentLength, "4")
            setBody("body")
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }
}
