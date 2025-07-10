package me.justlime.redeemXRestAPI.actions

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend fun ApplicationCall.respondForbidden(msg: String) {
    respond(HttpStatusCode.Forbidden, mapOf("error" to msg))
}

suspend fun ApplicationCall.respondBadRequest(msg: String) {
    respond(HttpStatusCode.BadRequest, mapOf("error" to msg))
}
