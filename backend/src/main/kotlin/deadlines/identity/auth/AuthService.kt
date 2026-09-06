package deadlines.identity.auth

import deadlines.identity.users.User
import deadlines.identity.users.UserAlreadyExistsException
import deadlines.identity.users.UserCredentialsRepository
import deadlines.identity.users.UserProfile
import deadlines.identity.users.UserRepository
import deadlines.identity.users.UserStatus
import deadlines.identity.users.toResponse
import deadlines.identity.email.EmailVerificationOperations
import java.time.Clock
import java.util.UUID

data class SessionContext(
    val userAgent: String?,
    val ipAddress: String?,
)

interface AuthOperations {
    suspend fun register(request: RegisterRequest, context: SessionContext): RegistrationResponse
    suspend fun login(request: LoginRequest, context: SessionContext): AuthResponse
    suspend fun refresh(refreshToken: String, context: SessionContext): AuthResponse
    suspend fun logout(refreshToken: String)
    suspend fun me(userId: UUID): deadlines.identity.users.UserResponse
}

class AuthService(
    private val credentials: UserCredentialsRepository,
    private val users: UserRepository,
    private val sessions: SessionRepository,
    private val passwordHasher: PasswordHasher,
    private val tokens: TokenService,
    private val emailVerification: EmailVerificationOperations,
    private val clock: Clock = Clock.systemUTC(),
) : AuthOperations {
    override suspend fun register(request: RegisterRequest, context: SessionContext): RegistrationResponse {
        val email = request.email.trim().lowercase()
        val firstName = request.firstName.trim()
        val lastName = request.lastName.trim()
        validateRegistration(email, request.password, firstName, lastName)

        if (credentials.findByEmail(email) != null || users.findByEmail(email) != null) {
            throw UserAlreadyExistsException()
        }

        val now = clock.instant()
        val user =
            User(
                id = UUID.randomUUID(),
                email = email,
                status = UserStatus.PENDING,
                profile = UserProfile(firstName, lastName, null, null),
                createdAt = now,
                updatedAt = now,
        )
        credentials.create(user, passwordHasher.hash(request.password))
        emailVerification.resend(user.id)
        return RegistrationResponse(user.toResponse())
    }

    override suspend fun login(request: LoginRequest, context: SessionContext): AuthResponse {
        val credentials = credentials.findByEmail(request.email.trim().lowercase())
        if (credentials == null || !passwordHasher.verify(request.password, credentials.passwordHash)) {
            throw InvalidCredentialsException()
        }
        if (credentials.user.status != UserStatus.ACTIVE) throw InvalidCredentialsException()

        return createSession(credentials.user, context)
    }

    override suspend fun refresh(refreshToken: String, context: SessionContext): AuthResponse {
        val now = clock.instant()
        val currentHash = tokens.hashRefreshToken(refreshToken)
        val current = sessions.findActive(currentHash, now) ?: throw InvalidRefreshTokenException()
        val user = users.findById(current.userId)
        if (user == null || user.status != UserStatus.ACTIVE) throw InvalidRefreshTokenException()

        val issued = tokens.issue(user.id)
        val replacement = issued.toSession(user.id, context, now)
        if (!sessions.rotate(currentHash, replacement, now)) throw InvalidRefreshTokenException()
        return issued.toResponse(user)
    }

    override suspend fun logout(refreshToken: String) {
        sessions.revoke(tokens.hashRefreshToken(refreshToken), clock.instant())
    }

    override suspend fun me(userId: UUID) =
        users.findById(userId)
            ?.takeIf { it.status == UserStatus.ACTIVE }
            ?.toResponse()
            ?: throw InvalidCredentialsException()

    private suspend fun createSession(user: User, context: SessionContext): AuthResponse {
        val now = clock.instant()
        val issued = tokens.issue(user.id)
        sessions.create(issued.toSession(user.id, context, now))
        return issued.toResponse(user)
    }

    private fun validateRegistration(email: String, password: String, firstName: String, lastName: String) {
        val violations = linkedMapOf<String, String>()
        if (!EMAIL_PATTERN.matches(email) || email.length > 320) violations["email"] = "must be a valid email address"
        if (password.length !in 12..72) violations["password"] = "must contain between 12 and 72 characters"
        if (firstName.isBlank() || firstName.length > 100) violations["firstName"] = "must contain between 1 and 100 characters"
        if (lastName.isBlank() || lastName.length > 100) violations["lastName"] = "must contain between 1 and 100 characters"
        if (violations.isNotEmpty()) throw AuthValidationException(violations)
    }

    private fun IssuedTokens.toSession(userId: UUID, context: SessionContext, now: java.time.Instant) =
        Session(UUID.randomUUID(), userId, refreshTokenHash, context.userAgent, context.ipAddress, refreshExpiresAt, now)

    private fun IssuedTokens.toResponse(user: User) =
        AuthResponse(accessToken, refreshToken, expiresIn = accessExpiresIn, user = user.toResponse())

    companion object {
        private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
