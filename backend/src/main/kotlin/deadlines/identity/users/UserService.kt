package deadlines.identity.users

import java.net.URI
import java.time.Clock
import java.util.UUID
import kotlin.math.ceil

class UserService(
    private val repository: UserRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun create(request: CreateUserRequest): User {
        val input = request.normalized()
        validate(input)

        if (repository.findByEmail(input.email) != null) {
            throw UserAlreadyExistsException()
        }

        val now = clock.instant()
        return repository.create(
            User(
                id = UUID.randomUUID(),
                email = input.email,
                status = UserStatus.ACTIVE,
                profile =
                    UserProfile(
                        firstName = input.firstName,
                        lastName = input.lastName,
                        avatarUrl = input.avatarUrl,
                        phone = input.phone,
                    ),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun get(id: UUID): User = repository.findById(id) ?: throw UserNotFoundException()

    suspend fun list(page: Int, limit: Int): UserListResponse {
        validatePagination(page, limit)
        val total = repository.count()
        val offset = (page - 1L) * limit
        val users = repository.list(offset, limit)

        return UserListResponse(
            data = users.map(User::toResponse),
            pagination =
                PaginationResponse(
                    page = page,
                    limit = limit,
                    total = total,
                    totalPages = if (total == 0L) 0 else ceil(total.toDouble() / limit).toInt(),
                ),
        )
    }

    suspend fun update(id: UUID, request: UpdateUserRequest): User {
        if (request.isEmpty()) {
            throw UserValidationException(mapOf("request" to "must contain at least one field"))
        }

        val current = get(id)
        val email = request.email?.normalizeEmail() ?: current.email
        val status = request.status?.parseStatus() ?: current.status
        val updated =
            current.copy(
                email = email,
                status = status,
                profile =
                    current.profile.copy(
                        firstName = request.firstName?.trim() ?: current.profile.firstName,
                        lastName = request.lastName?.trim() ?: current.profile.lastName,
                        avatarUrl = request.avatarUrl?.normalizeOptional() ?: current.profile.avatarUrl,
                        phone = request.phone?.normalizeOptional() ?: current.profile.phone,
                    ),
                updatedAt = clock.instant(),
            )

        validate(updated.toCreateRequest())
        if (email != current.email && repository.findByEmail(email) != null) {
            throw UserAlreadyExistsException()
        }

        return repository.update(updated)
    }

    suspend fun disable(id: UUID) {
        val current = get(id)
        if (current.status != UserStatus.DISABLED) {
            repository.update(current.copy(status = UserStatus.DISABLED, updatedAt = clock.instant()))
        }
    }

    private fun validate(request: CreateUserRequest) {
        val violations = linkedMapOf<String, String>()

        if (!EMAIL_PATTERN.matches(request.email) || request.email.length > 320) {
            violations["email"] = "must be a valid email address"
        }
        if (request.firstName.isBlank() || request.firstName.length > 100) {
            violations["firstName"] = "must contain between 1 and 100 characters"
        }
        if (request.lastName.isBlank() || request.lastName.length > 100) {
            violations["lastName"] = "must contain between 1 and 100 characters"
        }
        if (request.phone != null && request.phone.length > 32) {
            violations["phone"] = "must contain at most 32 characters"
        }
        if (request.avatarUrl != null && !request.avatarUrl.isHttpUrl()) {
            violations["avatarUrl"] = "must be an HTTP or HTTPS URL"
        }

        if (violations.isNotEmpty()) {
            throw UserValidationException(violations)
        }
    }

    private fun validatePagination(page: Int, limit: Int) {
        val violations = linkedMapOf<String, String>()
        if (page < 1) violations["page"] = "must be greater than or equal to 1"
        if (limit !in 1..100) violations["limit"] = "must be between 1 and 100"
        if (violations.isNotEmpty()) throw UserValidationException(violations)
    }

    private fun String.parseStatus(): UserStatus =
        UserStatus.entries.firstOrNull { it.name.equals(trim(), ignoreCase = true) }
            ?: throw UserValidationException(mapOf("status" to "must be pending, active or disabled"))

    private fun User.toCreateRequest() =
        CreateUserRequest(
            email = email,
            firstName = profile.firstName,
            lastName = profile.lastName,
            avatarUrl = profile.avatarUrl,
            phone = profile.phone,
        )

    private fun CreateUserRequest.normalized() =
        copy(
            email = email.normalizeEmail(),
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            avatarUrl = avatarUrl?.normalizeOptional(),
            phone = phone?.normalizeOptional(),
        )

    private fun String.normalizeEmail() = trim().lowercase()

    private fun String.normalizeOptional() = trim().ifEmpty { null }

    private fun String.isHttpUrl(): Boolean =
        runCatching { URI(this) }
            .getOrNull()
            ?.let { it.scheme in setOf("http", "https") && !it.host.isNullOrBlank() }
            ?: false

    private fun UpdateUserRequest.isEmpty() =
        email == null &&
            firstName == null &&
            lastName == null &&
            avatarUrl == null &&
            phone == null &&
            status == null

    companion object {
        private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
