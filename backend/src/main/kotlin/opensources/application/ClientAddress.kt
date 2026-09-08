package opensources.application

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin

class ClientAddressResolver(
    private val trustedProxies: Set<String>,
) {
    fun resolve(call: ApplicationCall): String {
        val direct = call.request.origin.remoteHost
        if (direct !in trustedProxies) return direct
        return call.request.headers["X-Forwarded-For"]
            ?.split(',')?.firstOrNull()?.trim()?.takeIf(String::isNotBlank)
            ?: direct
    }
}
