package deadlines.application

import deadlines.shared.errors.ApiErrorBody
import deadlines.shared.errors.ApiErrorResponse
import deadlines.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun Application.configurePlugins() {
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
