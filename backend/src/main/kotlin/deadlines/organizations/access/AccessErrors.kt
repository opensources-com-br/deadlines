package deadlines.organizations.access

import deadlines.shared.errors.ApiException

class PermissionNotFoundException : ApiException(404, "PERMISSION_NOT_FOUND", "Permission not found")

class PermissionAlreadyExistsException : ApiException(
    409,
    "PERMISSION_ALREADY_EXISTS",
    "A permission with this key already exists",
)

class SystemPermissionImmutableException : ApiException(
    409,
    "SYSTEM_PERMISSION_IMMUTABLE",
    "System permissions cannot be changed or deleted",
)

class RoleNotFoundException : ApiException(404, "ROLE_NOT_FOUND", "Role not found")

class RoleAlreadyExistsException : ApiException(
    409,
    "ROLE_ALREADY_EXISTS",
    "A role with this key already exists",
)

class SystemRoleImmutableException : ApiException(
    409,
    "SYSTEM_ROLE_IMMUTABLE",
    "System roles cannot be changed or deleted",
)

class OwnerPermissionsImmutableException : ApiException(
    409,
    "OWNER_PERMISSIONS_IMMUTABLE",
    "The owner role must retain every permission",
)

class AccessValidationException(
    violations: Map<String, String>,
) : ApiException(422, "VALIDATION_ERROR", "Invalid access control data", violations)
