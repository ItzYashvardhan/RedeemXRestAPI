package me.justlime.redeemXRestAPI.utilities

import me.justlime.redeemXRestAPI.enums.JConfig

fun validateToken(validToken: String): Boolean {
    val token = JService.config.getString(JConfig.API_TOKEN.path) ?: ""
    return token == validToken
}
