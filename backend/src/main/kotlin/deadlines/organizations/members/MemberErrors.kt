package deadlines.organizations.members

import deadlines.shared.errors.ApiException

class MemberNotFoundException : ApiException(404, "MEMBER_NOT_FOUND", "Organization member not found")

class OwnerMembershipImmutableException : ApiException(
    409,
    "OWNER_MEMBERSHIP_IMMUTABLE",
    "The organization owner cannot be reassigned or removed",
)

class MembershipStateConflictException : ApiException(
    409,
    "MEMBERSHIP_STATE_CONFLICT",
    "The membership is not in the required state",
)

class OwnerCannotLeaveException : ApiException(
    409,
    "OWNER_CANNOT_LEAVE",
    "Transfer organization ownership before leaving",
)

class MemberValidationException(
    violations: Map<String, String>,
) : ApiException(422, "VALIDATION_ERROR", "Invalid member data", violations)
