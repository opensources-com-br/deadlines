package deadlines.identity.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class EmailMessage(
    val to: String,
    val subject: String,
    val text: String,
)

interface EmailService {
    suspend fun send(message: EmailMessage)
}

class LoggingEmailService : EmailService {
    private val logger = LoggerFactory.getLogger(LoggingEmailService::class.java)

    override suspend fun send(message: EmailMessage) {
        logger.info("Local email queued: to={}, subject={}", message.to, message.subject)
    }
}

data class EmailHttpResponse(
    val statusCode: Int,
)

interface EmailHttpTransport {
    suspend fun post(url: String, headers: Map<String, String>, body: String): EmailHttpResponse
}

class JdkEmailHttpTransport(
    private val client: HttpClient = HttpClient.newHttpClient(),
) : EmailHttpTransport {
    override suspend fun post(url: String, headers: Map<String, String>, body: String): EmailHttpResponse =
        withContext(Dispatchers.IO) {
            val request =
                HttpRequest.newBuilder(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .apply { headers.forEach { (name, value) -> header(name, value) } }
                    .build()
            EmailHttpResponse(client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode())
        }
}

class ResendEmailService(
    private val apiKey: String,
    private val from: String,
    private val transport: EmailHttpTransport = JdkEmailHttpTransport(),
) : EmailService {
    override suspend fun send(message: EmailMessage) {
        val payload =
            buildJsonObject {
                put("from", from)
                put("to", message.to)
                put("subject", message.subject)
                put("text", message.text)
            }.toString()
        val response =
            transport.post(
                url = "https://api.resend.com/emails",
                headers =
                    mapOf(
                        "Authorization" to "Bearer $apiKey",
                        "Content-Type" to "application/json",
                    ),
                body = payload,
            )
        if (response.statusCode !in 200..299) throw EmailDeliveryException()
    }
}

class RecordingEmailService : EmailService {
    private val messages = mutableListOf<EmailMessage>()

    val sentMessages: List<EmailMessage>
        get() = messages.toList()

    override suspend fun send(message: EmailMessage) {
        messages += message
    }
}
