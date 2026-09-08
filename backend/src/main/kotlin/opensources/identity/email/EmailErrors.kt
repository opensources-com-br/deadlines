package opensources.identity.email

import opensources.shared.errors.ApiException

class InvalidEmailVerificationTokenException : ApiException(
    status = 400,
    code = "INVALID_EMAIL_VERIFICATION_TOKEN",
    message = "Email verification token is invalid or expired",
)
