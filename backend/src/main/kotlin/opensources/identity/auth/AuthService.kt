package opensources.identity.auth

import opensources.identity.users.User
import opensources.identity.users.UserAlreadyExistsException
import opensources.identity.users.UserCredentialsRepository
import opensources.identity.users.UserProfile
import opensources.identity.users.UserRepository
import opensources.identity.users.UserStatus
import opensources.identity.users.ActiveAccountOperations
import opensources.identity.users.ActiveAccountService
import opensources.identity.users.toResponse
import opensources.identity.email.EmailVerificationOperations
import java.time.Clock
import java.util.UUID

data class SessionContext(
    val userAgent: String?,
    val ipAddress: String?,
    val deviceId: UUID? = null,
)

interface AuthOperations {
    suspend fun register(request: RegisterRequest, context: SessionContext): RegistrationResponse
    suspend fun login(request: LoginRequest, context: SessionContext): AuthResponse
    suspend fun reactivate(request: ReactivateAccountRequest, context: SessionContext): AuthResponse
    suspend fun refresh(refreshToken: String, context: SessionContext): AuthResponse
    suspend fun logout(refreshToken: String)
    suspend fun me(userId: UUID): opensources.identity.users.UserResponse
    suspend fun changePassword(userId: UUID, request: ChangePasswordRequest, context: SessionContext): AuthResponse
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
    private val activeAccounts: ActiveAccountOperations = ActiveAccountService(users)
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
        if (!activeAccounts.isActive(credentials.user.id)) throw InvalidCredentialsException()

        return createSession(credentials.user, context)
    }

    override suspend fun reactivate(request: ReactivateAccountRequest, context: SessionContext): AuthResponse {
        val credentials = credentials.findByEmail(request.email.trim().lowercase()) ?: throw InvalidCredentialsException()
        if (!passwordHasher.verify(request.password, credentials.passwordHash)) throw InvalidCredentialsException()
        val user = credentials.user
        if (user.status != UserStatus.DISABLED || user.emailVerifiedAt == null) throw InvalidCredentialsException()

        val now = clock.instant()
        val reactivated = users.update(user.copy(status = UserStatus.ACTIVE, disabledAt = null, updatedAt = now))
        sessions.revokeAll(user.id, now)
        return createSession(reactivated, context)
    }

    override suspend fun refresh(refreshToken: String, context: SessionContext): AuthResponse {
        val now = clock.instant()
        val currentHash = tokens.hashRefreshToken(refreshToken)
        val current = sessions.findActive(currentHash, now) ?: throw InvalidRefreshTokenException()
        val user = users.findById(current.userId)?.takeIf { activeAccounts.isActive(it.id) }
            ?: throw InvalidRefreshTokenException()

        val issued = tokens.issue(user.id, current.id)
        val replacement = issued.toSession(
            userId = user.id,
            context = context,
            now = now,
            deviceId = current.deviceId,
            createdAt = current.createdAt,
        )
        if (!sessions.rotate(currentHash, replacement, now)) throw InvalidRefreshTokenException()
        return issued.toResponse(user)
    }

    override suspend fun logout(refreshToken: String) {
        sessions.revoke(tokens.hashRefreshToken(refreshToken), clock.instant())
    }

    override suspend fun me(userId: UUID) =
        try {
            activeAccounts.requireActive(userId).toResponse()
        } catch (_: opensources.identity.users.AccountNotActiveException) {
            throw InvalidCredentialsException()
        }

    override suspend fun changePassword(userId: UUID, request: ChangePasswordRequest, context: SessionContext): AuthResponse {
        if (request.newPassword.length !in 12..72) {
            throw AuthValidationException(mapOf("newPassword" to "must contain between 12 and 72 characters"))
        }

        val user = try {
            activeAccounts.requireActive(userId)
        } catch (_: opensources.identity.users.AccountNotActiveException) {
            throw InvalidCredentialsException()
        }
        val current = credentials.findByEmail(user.email) ?: throw InvalidCredentialsException()
        if (!passwordHasher.verify(request.currentPassword, current.passwordHash)) {
            throw InvalidCurrentPasswordException()
        }

        val now = clock.instant()
        val currentSession = sessions.findActive(tokens.hashRefreshToken(request.refreshToken), now)
        if (currentSession?.userId != userId) throw InvalidRefreshTokenException()
        if (!credentials.updatePassword(userId, passwordHasher.hash(request.newPassword), now)) {
            throw InvalidCredentialsException()
        }
        sessions.revokeAll(userId, now)
        return createSession(user, context)
    }

    private suspend fun createSession(user: User, context: SessionContext): AuthResponse {
        val now = clock.instant()
        val deviceId = context.deviceId
            ?: throw AuthValidationException(mapOf("X-Device-Id" to "is required and must be a UUID"))
        val existing = sessions.findByDevice(user.id, deviceId)
        val issued = tokens.issue(user.id, existing?.id ?: UUID.randomUUID())
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

    private fun IssuedTokens.toSession(
        userId: UUID,
        context: SessionContext,
        now: java.time.Instant,
        deviceId: UUID = context.deviceId
            ?: throw AuthValidationException(mapOf("X-Device-Id" to "is required and must be a UUID")),
        createdAt: java.time.Instant = now,
    ) =
        Session(
            id = sessionId,
            userId = userId,
            refreshTokenHash = refreshTokenHash,
            userAgent = context.userAgent,
            ipAddress = context.ipAddress,
            expiresAt = refreshExpiresAt,
            createdAt = createdAt,
            deviceId = deviceId,
            lastSeenAt = now,
        )

    private fun IssuedTokens.toResponse(user: User) =
        AuthResponse(accessToken, refreshToken, expiresIn = accessExpiresIn, user = user.toResponse())

    companion object {
        private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
