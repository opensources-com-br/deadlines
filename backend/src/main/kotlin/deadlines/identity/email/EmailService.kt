package deadlines.identity.email

import org.slf4j.LoggerFactory

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

class RecordingEmailService : EmailService {
    private val messages = mutableListOf<EmailMessage>()

    val sentMessages: List<EmailMessage>
        get() = messages.toList()

    override suspend fun send(message: EmailMessage) {
        messages += message
    }
}
