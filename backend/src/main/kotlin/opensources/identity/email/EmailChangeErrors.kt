package opensources.identity.email

import opensources.shared.errors.ApiException

class InvalidEmailChangeTokenException : ApiException(400, "INVALID_EMAIL_CHANGE_TOKEN", "Email change token is invalid or expired")

class EmailChangeValidationException : ApiException(
    422,
    "VALIDATION_ERROR",
    "Invalid email change request",
    mapOf("email" to "must be a valid email address"),
)
