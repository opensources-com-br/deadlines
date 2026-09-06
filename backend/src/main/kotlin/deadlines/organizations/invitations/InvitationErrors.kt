package deadlines.organizations.invitations

import deadlines.shared.errors.ApiException

class InvitationNotFoundException : ApiException(404, "INVITATION_NOT_FOUND", "Invitation not found")

class InvitationAlreadyExistsException : ApiException(
    409,
    "INVITATION_ALREADY_EXISTS",
    "A pending invitation already exists for this email",
)

class InvitationInvalidException : ApiException(422, "INVITATION_INVALID", "Invitation is invalid or has expired")

class InvitationEmailMismatchException : ApiException(
    403,
    "INVITATION_EMAIL_MISMATCH",
    "This invitation belongs to another email address",
)

class InvitationForMemberException : ApiException(
    409,
    "INVITATION_FOR_MEMBER",
    "This user is already a member of the organization",
)

class InvitationValidationException(
    violations: Map<String, String>,
) : ApiException(422, "VALIDATION_ERROR", "Invalid invitation data", violations)
