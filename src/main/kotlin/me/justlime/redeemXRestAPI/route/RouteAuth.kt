package me.justlime.redeemXRestAPI.route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import me.justlime.redeemXRestAPI.rxrPlugin
import me.justlime.redeemXRestAPI.utilities.validateToken

val AuthAndLoggingPlugin = createRouteScopedPlugin("AuthAndLogging") {
    onCall { call ->
        val path = call.request.path()
        val method = call.request.httpMethod.value
        rxrPlugin.logger.info("Incoming API Request: $method $path")

        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
        if (token == null || !validateToken(token)) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Unauthorized or invalid token"))
            return@onCall
        }
    }
}