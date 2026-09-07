package deadlines.identity.users

import deadlines.shared.errors.ApiException

class UserNotFoundException : ApiException(
    status = 404,
    code = "USER_NOT_FOUND",
    message = "User not found",
)

class UserAlreadyExistsException : ApiException(
    status = 409,
    code = "USER_ALREADY_EXISTS",
    message = "A user with this email already exists",
)

class UserValidationException(
    violations: Map<String, String>,
) : ApiException(
        status = 422,
        code = "VALIDATION_ERROR",
        message = "Invalid user data",
    details = violations,
)

class AccountNotActiveException : ApiException(
    status = 409,
    code = "ACCOUNT_NOT_ACTIVE",
    message = "Only an active account can perform this action",
)

class AccountOwnerConflictException : ApiException(
    status = 409,
    code = "ACCOUNT_OWNER_CONFLICT",
    message = "Transfer organization ownership or delete the organization first",
)
