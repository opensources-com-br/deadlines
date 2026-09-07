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

class OwnershipTransferDeniedException : ApiException(403, "OWNERSHIP_TRANSFER_DENIED", "Only the owner can transfer ownership")

class OwnershipTransferTargetException : ApiException(409, "OWNERSHIP_TRANSFER_TARGET_INVALID", "The new owner must be another active member")

class OwnershipTransferRoleException : ApiException(409, "OWNERSHIP_TRANSFER_ROLE_INVALID", "Choose a non-owner role for the previous owner")

class OwnershipInvariantException : ApiException(500, "OWNERSHIP_INVARIANT_VIOLATION", "The organization owner role is unavailable")

class MemberValidationException(
    violations: Map<String, String>,
) : ApiException(422, "VALIDATION_ERROR", "Invalid member data", violations)
