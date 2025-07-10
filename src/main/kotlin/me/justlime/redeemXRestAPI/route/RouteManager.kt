package me.justlime.redeemXRestAPI.route

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.routeManager() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; isLenient = true })
    }

    routing {
        route("/api/rcx") {
            install(AuthAndLoggingPlugin)
            generateRoute()
            deleteRoute()
        }
    }
}

