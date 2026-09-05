package deadlines.application

import deadlines.identity.auth.TokenService
import deadlines.shared.errors.ApiErrorBody
import deadlines.shared.errors.ApiErrorResponse
import deadlines.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.util.UUID

fun Application.configurePlugins(tokenService: TokenService? = null) {
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
                        ApiErrorResponse(ApiErrorBody("UNAUTHORIZED", "Authentication is required")),
                    )
                }
            }
        }
    }

    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ApiErrorResponse(
                    error = ApiErrorBody(
                        code = "NOT_FOUND",
                        message = "Resource not found",
                    ),
                ),
            )
        }

        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiErrorResponse(
                    error = ApiErrorBody(
                        code = "INVALID_REQUEST",
                        message = "Request body is invalid",
                    ),
                ),
            )
        }

        exception<ApiException> { call, cause ->
            call.respond(
                HttpStatusCode.fromValue(cause.status),
                ApiErrorResponse(
                    error = ApiErrorBody(
                        code = cause.code,
                        message = cause.message,
                        details = cause.details,
                    ),
                ),
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled request failure", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorResponse(
                    error = ApiErrorBody(
                        code = "INTERNAL_ERROR",
                        message = "An unexpected error occurred",
                    ),
                ),
            )
        }
    }
}
