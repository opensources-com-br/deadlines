package deadlines.identity.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    private val hasher = BcryptPasswordHasher(cost = 10)

    @Test
    fun `hashes and verifies a password`() {
        val hash = hasher.hash("a-valid-password")

        assertNotEquals("a-valid-password", hash)
        assertTrue(hasher.verify("a-valid-password", hash))
        assertFalse(hasher.verify("wrong-password", hash))
    }

    @Test
    fun `generates a unique salt for each hash`() {
        val first = hasher.hash("a-valid-password")
        val second = hasher.hash("a-valid-password")

        assertNotEquals(first, second)
    }
}
