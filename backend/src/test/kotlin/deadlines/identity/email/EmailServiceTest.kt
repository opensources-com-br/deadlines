package deadlines.identity.email

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EmailServiceTest {
    @Test
    fun `records local messages for tests`() =
        runTest {
            val service = RecordingEmailService()
            val message = EmailMessage("user@example.com", "Confirm your email", "Confirmation content")

            service.send(message)

            assertEquals(listOf(message), service.sentMessages)
        }
}
