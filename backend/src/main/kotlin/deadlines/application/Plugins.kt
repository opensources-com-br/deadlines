package deadlines.application

import deadlines.identity.auth.TokenService
import deadlines.shared.errors.ApiErrorBody
import deadlines.shared.errors.ApiErrorResponse
import deadlines.shared.errors.ApiException
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
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.intercept
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.util.UUID

fun Application.configurePlugins(tokenService: TokenService? = null) {
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
            val status = call.response.status()?.value ?: "pending"
            "${call.request.httpMethod.value} status=$status"
        }
    }

    install(CORS) {
        allowHost("localhost:3000", schemes = listOf("http"))
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(REQUEST_ID_HEADER)
        exposeHeader(REQUEST_ID_HEADER)
    }

    if (tokenService != null) {
        install(Authentication) {
            jwt("auth-jwt") {
                verifier(tokenService.verifier())
                validate { credential ->
                    val subject = credential.payload.subject
                    if (subject != null && runCatching { UUID.fromString(subject) }.isSuccess) {
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
            call.application.log.error("Unhandled request failure", cause)
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
