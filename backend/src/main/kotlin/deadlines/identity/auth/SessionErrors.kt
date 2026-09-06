package deadlines.identity.auth

import deadlines.shared.errors.ApiException

class SessionNotFoundException : ApiException(
    status = 404,
    code = "SESSION_NOT_FOUND",
    message = "Session not found",
)

class SessionValidationException : ApiException(
    status = 422,
    code = "VALIDATION_ERROR",
    message = "Invalid session data",
    details = mapOf("sessionId" to "must be a valid UUID"),
)
