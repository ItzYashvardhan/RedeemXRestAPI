package me.justlime.redeemXRestAPI.route

import api.justlime.redeemcodex.RedeemXAPI
import api.justlime.redeemcodex.models.RedeemCode
import api.justlime.redeemcodex.models.RedeemTemplate
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.justlime.redeemXRestAPI.models.GenerateResponse
import me.justlime.redeemXRestAPI.rxrPlugin

fun Route.generateRoute() {
    post("/generate/code") {
        try {
            val params = call.receiveParameters()
            val result = handleCodeGeneration(params)
            call.respond(result)
        } catch (e: Exception) {
            rxrPlugin.logger.warning("An error occurred in /generateCode route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal Server Error", "details" to e.localizedMessage))
        }
    }
    post("/generate/template") {
        try {
            val params = call.receiveParameters()
            val result = handleTemplateGeneration(params)
            call.respond(result)
        } catch (e: Exception) {
            rxrPlugin.logger.warning("An error occurred in /generateCode route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal Server Error", "details" to e.localizedMessage))
        }
    }
}


fun handleCodeGeneration(
    params: Parameters
): GenerateResponse {

    val digit = params["digit"]?.toIntOrNull()
    val custom = params["custom"]?.split(" ")
    val amount = params["amount"]?.toIntOrNull() ?: 1
    val template = params["template"] ?: "DEFAULT"

    if (digit == null && custom == null) {
        return GenerateResponse(
            success = HttpStatusCode.BadRequest.value,
            error = "Missing required parameters: 'digit' or 'custom'"
        )
    }
    val generatedCode = mutableSetOf<RedeemCode>()

    if (digit != null) {
        val digitCode = RedeemXAPI.code.generateCode(digit, template, amount)
        generatedCode.addAll(digitCode)
    }
    if (custom != null) {
        val customCode = custom.map { RedeemXAPI.code.generateCode(it, template) }
        generatedCode.addAll(customCode)
    }
    if (generatedCode.isEmpty()) {
        return GenerateResponse(
            success = HttpStatusCode.BadGateway.value,
            error = "No codes generated."
        )
    }
    val codes = generatedCode.map { it.code }
    return GenerateResponse(
        success = HttpStatusCode.OK.value,
        result = codes,
    )
}

fun handleTemplateGeneration(
    params: Parameters
): GenerateResponse {
    val name = params["template"]?.split(" ")

    val generatedTemplate = mutableSetOf<RedeemTemplate>()
    if (name == null) {
        return GenerateResponse(
            success = HttpStatusCode.BadGateway.value,
            error = "No template name provided."
        )
    }
    name.map {
        if (RedeemXAPI.template.isTemplateExist(it)) {
            return GenerateResponse(
                success = HttpStatusCode.BadGateway.value,
                error = "Template already exist."
            )
        }
    }

    val templates = name.map { RedeemXAPI.template.generateTemplate(it) }
    generatedTemplate.addAll(templates)
    if (generatedTemplate.isEmpty()) {
        return GenerateResponse(
            success = HttpStatusCode.BadGateway.value,
            error = "No template Generated."
        )
    }
    val template = generatedTemplate.map { it.name }
    return GenerateResponse(
        success = HttpStatusCode.OK.value,
        result = template,
    )
}
