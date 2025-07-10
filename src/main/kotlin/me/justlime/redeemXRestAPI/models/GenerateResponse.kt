package me.justlime.redeemXRestAPI.models

import kotlinx.serialization.Serializable

@Serializable
data class GenerateResponse(
    val success: Int,
    val result: List<String>? = null,
    val error: String? = null
)
