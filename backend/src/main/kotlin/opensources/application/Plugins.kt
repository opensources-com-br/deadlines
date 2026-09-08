package opensources.application

import opensources.identity.auth.TokenService
import opensources.identity.users.ActiveAccountOperations
import opensources.config.HttpConfig
import opensources.shared.errors.ApiErrorBody
import opensources.shared.errors.ApiErrorResponse
import opensources.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.intercept
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.slf4j.event.Level
import java.util.UUID

fun Application.configurePlugins(
    tokenService: TokenService? = null,
    activeAccounts: ActiveAccountOperations? = null,
    http: HttpConfig = HttpConfig(8080),
) {
    intercept(ApplicationCallPipeline.Setup) {
        val requestId = call.request.headers[REQUEST_ID_HEADER]?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        call.attributes.put(RequestIdKey, requestId)
        call.response.headers.append(REQUEST_ID_HEADER, requestId)
        call.attributes.put(RequestStartedAtKey, System.nanoTime())
    }

    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false
                ignoreUnknownKeys = false
            },
        )
    }

    install(CallLogging) {
        level = Level.INFO
        format { call ->
            structuredLogJson.encodeToString(
                RequestLog(
                    requestId = call.requestId(),
                    method = call.request.httpMethod.value,
                    path = call.request.path(),
                    status = call.response.status()?.value,
                    duration = call.requestDurationMillis(),
                    userId = call.principal<JWTPrincipal>()?.payload?.subject,
                ),
            )
        }
    }

    install(CORS) {
        http.allowedCorsOrigins.forEach { origin ->
            val parsed = java.net.URI(origin)
            val host = parsed.host ?: throw IllegalArgumentException("CORS_ALLOWED_ORIGINS must contain valid origins")
            val hostWithPort = if (parsed.port == -1) host else "$host:${parsed.port}"
            allowHost(hostWithPort, schemes = listOf(parsed.scheme ?: throw IllegalArgumentException("CORS origin requires a scheme")))
        }
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Origin)
        allowHeader(REQUEST_ID_HEADER)
        exposeHeader(REQUEST_ID_HEADER)
    }

    if (tokenService != null) {
        install(Authentication) {
            jwt("auth-jwt") {
                verifier(tokenService.verifier())
                validate { credential ->
                    val subject = credential.payload.subject
                    val userId = subject?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    if (userId != null && (activeAccounts == null || activeAccounts.isActive(userId))) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
                challenge { _, _ ->
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        call.apiErrorResponse("UNAUTHORIZED", "Authentication is required"),
                    )
                }
            }
        }
    }

    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                call.apiErrorResponse("NOT_FOUND", "Resource not found"),
            )
        }

        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                call.apiErrorResponse("INVALID_REQUEST", "Request body is invalid"),
            )
        }

        exception<ApiException> { call, cause ->
            call.respond(
                HttpStatusCode.fromValue(cause.status),
                call.apiErrorResponse(cause.code, cause.message, cause.details),
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled request failure requestId={}", call.requestId(), cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                call.apiErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"),
            )
        }
    }
}

private fun ApplicationCall.apiErrorResponse(
    code: String,
    message: String,
    fields: Map<String, String> = emptyMap(),
): ApiErrorResponse {
    return ApiErrorResponse(ApiErrorBody(code, message, fields, requestId()))
}

internal fun ApplicationCall.requestId(): String = attributes[RequestIdKey]

internal fun ApplicationCall.requestDurationMillis(): Long =
    (System.nanoTime() - attributes[RequestStartedAtKey]) / 1_000_000

private const val REQUEST_ID_HEADER = "X-Request-Id"
private val RequestIdKey = AttributeKey<String>("request-id")
private val RequestStartedAtKey = AttributeKey<Long>("request-started-at")
private val structuredLogJson = Json { encodeDefaults = true; explicitNulls = true }

@Serializable
private data class RequestLog(
    val timestamp: String = java.time.Instant.now().toString(),
    val level: String = "INFO",
    val requestId: String,
    val method: String,
    val path: String,
    val status: Int?,
    val duration: Long,
    val userId: String?,
    val organizationId: String? = null,
)
