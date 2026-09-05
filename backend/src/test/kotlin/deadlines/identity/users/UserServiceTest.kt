package deadlines.identity.users

import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserServiceTest {
    private val repository = InMemoryUserRepository()
    private val clock = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC)
    private val service = UserService(repository, clock)

    @Test
    fun `creates a normalized active user`() =
        runTest {
            val user = service.create(validRequest.copy(email = "  TARIK@Example.com "))

            assertEquals("tarik@example.com", user.email)
            assertEquals(UserStatus.ACTIVE, user.status)
            assertEquals("Tarik", user.profile.firstName)
            assertEquals(clock.instant(), user.createdAt)
        }

    @Test
    fun `reports all invalid fields together`() =
        runTest {
            val error =
                assertFailsWith<UserValidationException> {
                    service.create(
                        CreateUserRequest(
                            email = "invalid",
                            firstName = " ",
                            lastName = "",
                            avatarUrl = "not-a-url",
                        ),
                    )
                }

            assertEquals(setOf("email", "firstName", "lastName", "avatarUrl"), error.details.keys)
        }

    @Test
    fun `rejects an email that already exists regardless of case`() =
        runTest {
            service.create(validRequest)

            assertFailsWith<UserAlreadyExistsException> {
                service.create(validRequest.copy(email = "TARIK@EXAMPLE.COM"))
            }
        }

    @Test
    fun `paginates users`() =
        runTest {
            repeat(3) { index ->
                service.create(validRequest.copy(email = "user$index@example.com"))
            }

            val result = service.list(page = 2, limit = 2)

            assertEquals(1, result.data.size)
            assertEquals(3, result.pagination.total)
            assertEquals(2, result.pagination.totalPages)
        }

    @Test
    fun `updates user data`() =
        runTest {
            val created = service.create(validRequest)

            val updated =
                service.update(
                    created.id,
                    UpdateUserRequest(firstName = "  T. ", status = "disabled"),
                )

            assertEquals("T.", updated.profile.firstName)
            assertEquals(UserStatus.DISABLED, updated.status)
        }

    @Test
    fun `delete behavior disables the user`() =
        runTest {
            val created = service.create(validRequest)

            service.disable(created.id)

            assertEquals(UserStatus.DISABLED, service.get(created.id).status)
        }

    @Test
    fun `reports a missing user`() =
        runTest {
            assertFailsWith<UserNotFoundException> {
                service.get(UUID.randomUUID())
            }
        }

    private companion object {
        val validRequest =
            CreateUserRequest(
                email = "tarik@example.com",
                firstName = "Tarik",
                lastName = "Villalobos",
            )
    }
}

private class InMemoryUserRepository : UserRepository {
    private val users = mutableListOf<User>()

    override suspend fun create(user: User): User = user.also(users::add)

    override suspend fun findById(id: UUID): User? = users.firstOrNull { it.id == id }

    override suspend fun findByEmail(email: String): User? =
        users.firstOrNull { it.email.equals(email, ignoreCase = true) }

    override suspend fun list(offset: Long, limit: Int): List<User> =
        users.drop(offset.toInt()).take(limit)

    override suspend fun count(): Long = users.size.toLong()

    override suspend fun update(user: User): User {
        users[users.indexOfFirst { it.id == user.id }] = user
        return user
    }
}
