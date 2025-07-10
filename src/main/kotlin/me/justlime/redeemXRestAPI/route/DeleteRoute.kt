package me.justlime.redeemXRestAPI.route

import api.justlime.redeemcodex.RedeemXAPI
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.justlime.redeemXRestAPI.models.Response
import me.justlime.redeemXRestAPI.rxrPlugin

fun Route.deleteRoute() {
    post("/delete/code") {
        try {
            val params = call.receiveParameters()
            val result = handleCodeDeletion(params)
            call.respond(result)
        } catch (e: Exception) {
            rxrPlugin.logger.warning("An error occurred in /modify/delete/code route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal Server Error", "details" to e.localizedMessage))
        }
    }
    post("/delete/template") {
        try {
            val params = call.receiveParameters()
            val result = handleTemplateDeletion(params)
            call.respond(result)
        } catch (e: Exception) {
            rxrPlugin.logger.warning("An error occurred in /modify/delete/template route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal Server Error", "details" to e.localizedMessage))
        }
    }
}

fun handleCodeDeletion(params: Parameters): Response {
    val codesToDelete = params["code"]?.split(" ") ?: emptyList()
    val templateNames = params["template"]?.split(" ") ?: emptyList()
    val deletedCodes = mutableSetOf<String>()
    var deletedCount = 0
    var templateDeletedCount = 0

    // Delete template-based codes, skipping already-deleted ones
    for (template in templateNames) {
        val redeemCodes = RedeemXAPI.code.getCodes().mapNotNull { RedeemXAPI.code.getCode(it) }
        val codesInTemplate = redeemCodes.filter { it.template.equals(template, ignoreCase = true) }
        codesInTemplate.forEach { code ->
            if (RedeemXAPI.code.deleteCode(code.code)) {
                deletedCodes.add(code.code)
                templateDeletedCount++
            }
        }
    }

    // Delete individual codes
    codesToDelete.forEach {
        if (RedeemXAPI.code.deleteCode(it)) {
            deletedCodes.add(it)
            deletedCount++
        }
    }

    if (deletedCount == 0 && templateDeletedCount == 0) {
        return Response(
            success = HttpStatusCode.NotFound.value,
            error = "No codes found to delete."
        )
    }

    val resultMessages = mutableListOf<String>()
    if (deletedCount > 0) resultMessages.add("Deleted $deletedCount code(s).")
    if (templateDeletedCount > 0) resultMessages.add("Deleted $templateDeletedCount code(s) from template(s): ${templateNames.joinToString()}.")

    return Response(
        success = HttpStatusCode.OK.value,
        result = resultMessages
    )
}

fun handleTemplateDeletion(params: Parameters): Response{
    val templateNames = params["template"]?.split(" ") ?: emptyList()
    val deletedTemplates = mutableSetOf<String>()
    var deletedCount = 0
    var deltedCodeCount =0

    if (templateNames.isEmpty()) {
        return Response(
            success = HttpStatusCode.BadRequest.value,
            error = "No template names provided for deletion."
        )
    }
    val redeemCodes = RedeemXAPI.code.getCodes().mapNotNull { RedeemXAPI.code.getCode(it) }

    templateNames.forEach { templateName ->
        if (RedeemXAPI.template.isTemplateExist(templateName)) {
            val codesInTemplate = redeemCodes.filter { it.template.equals(templateName, ignoreCase = true) }
            codesInTemplate.forEach { code ->
                if (RedeemXAPI.code.deleteCode(code.code)) {
                    deltedCodeCount++
                }
            }

            if (RedeemXAPI.template.deleteTemplate(templateName)) {
                deletedTemplates.add(templateName)
                deletedCount++
            }
        }
    }

    return if (deletedCount > 0) {
        Response(
            success = HttpStatusCode.OK.value,
            result = listOf("Deleted $deletedCount template(s): ${deletedTemplates.joinToString()} with $deltedCodeCount code(s)"),
        )
    } else {
        Response(
            success = HttpStatusCode.NotFound.value,
            error = "No templates found to delete or templates could not be deleted."
        )
    }
}