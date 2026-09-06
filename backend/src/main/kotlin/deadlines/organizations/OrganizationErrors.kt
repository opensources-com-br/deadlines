package deadlines.organizations

import deadlines.shared.errors.ApiException

class OrganizationNotFoundException : ApiException(
    status = 404,
    code = "ORGANIZATION_NOT_FOUND",
    message = "Organization not found",
)

class OrganizationAlreadyExistsException : ApiException(
    status = 409,
    code = "ORGANIZATION_ALREADY_EXISTS",
    message = "An organization with this slug already exists",
)

class ActiveMembershipAlreadyExistsException : ApiException(
    status = 409,
    code = "ACTIVE_MEMBERSHIP_ALREADY_EXISTS",
    message = "User already belongs to an organization",
)

class OrganizationAccessDeniedException : ApiException(
    status = 403,
    code = "ORGANIZATION_ACCESS_DENIED",
    message = "Only the organization owner can perform this action",
)

class OrganizationValidationException(
    violations: Map<String, String>,
) : ApiException(
        status = 422,
        code = "VALIDATION_ERROR",
        message = "Invalid organization data",
        details = violations,
    )
