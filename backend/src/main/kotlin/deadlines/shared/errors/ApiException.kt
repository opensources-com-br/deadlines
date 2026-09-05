package deadlines.shared.errors

import kotlinx.serialization.Serializable

open class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
    val details: Map<String, String> = emptyMap(),
) : RuntimeException(message)

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorBody,
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)
