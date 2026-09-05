package deadlines.identity.auth

import deadlines.shared.errors.ApiException

class InvalidCredentialsException : ApiException(
    status = 401,
    code = "INVALID_CREDENTIALS",
    message = "Email or password is invalid",
)

class InvalidRefreshTokenException : ApiException(
    status = 401,
    code = "INVALID_REFRESH_TOKEN",
    message = "Refresh token is invalid or expired",
)

class AuthValidationException(
    violations: Map<String, String>,
) : ApiException(
        status = 422,
        code = "VALIDATION_ERROR",
        message = "Invalid authentication data",
        details = violations,
    )
