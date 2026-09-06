package deadlines.identity.email

import deadlines.shared.errors.ApiException

class EmailDeliveryException : ApiException(
    status = 503,
    code = "EMAIL_DELIVERY_UNAVAILABLE",
    message = "Email delivery is temporarily unavailable",
)
