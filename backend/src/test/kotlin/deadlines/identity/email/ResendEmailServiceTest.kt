package deadlines.identity.email

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResendEmailServiceTest {
    @Test
    fun `sends the Resend payload with the configured sender`() =
        runTest {
            val transport = RecordingTransport(EmailHttpResponse(200))
            val service = ResendEmailService("re_test", "Deadlines <onboarding@resend.dev>", transport)

            service.send(EmailMessage("tarik@example.com", "Confirm your email", "Use this link."))

            val payload = Json.parseToJsonElement(transport.body).jsonObject
            assertEquals("https://api.resend.com/emails", transport.url)
            assertEquals("Bearer re_test", transport.headers["Authorization"])
            assertEquals("Deadlines <onboarding@resend.dev>", payload["from"]!!.jsonPrimitive.content)
            assertEquals("tarik@example.com", payload["to"]!!.jsonPrimitive.content)
        }

    @Test
    fun `maps an unsuccessful Resend response to an API error`() =
        runTest {
            val service = ResendEmailService("re_test", "onboarding@resend.dev", RecordingTransport(EmailHttpResponse(401)))

            assertFailsWith<EmailDeliveryException> {
                service.send(EmailMessage("tarik@example.com", "Subject", "Text"))
            }
        }
}

private class RecordingTransport(
    private val response: EmailHttpResponse,
) : EmailHttpTransport {
    var url: String? = null
    var headers: Map<String, String> = emptyMap()
    var body: String = ""

    override suspend fun post(url: String, headers: Map<String, String>, body: String): EmailHttpResponse {
        this.url = url
        this.headers = headers
        this.body = body
        return response
    }
}
