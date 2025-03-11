package me.justlime.redeemXRestAPI

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import me.justlime.redeemcodex.api.RedeemXAPI
import me.justlime.redeemcodex.commands.RCXCommand
import me.justlime.redeemcodex.enums.JProperty
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.math.BigInteger
import java.net.BindException
import java.security.SecureRandom
import java.util.*

class RedeemXRestAPI : JavaPlugin() {
    private var ktorServer: NettyApplicationEngine? = null
    private lateinit var apiToken: String
    private var apiEnabled: Boolean = false
    private var apiPort: Int = 8080
    private var apiHost: String = "0.0.0.0"
    private var api = RedeemXAPI

    override fun onEnable() {
        // Load the default config if it doesn't exist.
        saveDefaultConfig()

        // Retrieve API settings from the config file.
        apiEnabled = config.getBoolean("api.enabled", false)
        apiToken = config.getString("api.token") ?: ""
        apiPort = config.getInt("port", 8080)
        apiHost = config.getString("host", "0.0.0.0") ?: "0.0.0.0"

        // Auto-generate a token if none exists.
        if (apiToken.isBlank()) {
            apiToken = generateToken()
            config.set("api.token", apiToken)
            saveConfig()
        }

        logger.info("RedeemXRestAPI API Enabled: $apiEnabled")
        logger.info("RedeemXRestAPI Token: $apiToken")

        // Start the REST API if enabled in the config.
        if (apiEnabled) {
            startRestApi()
        }
    }

    private fun startRestApi() {
        try {
            ktorServer = embeddedServer(Netty, port = apiPort, host = apiHost) {
                module()
            }.start(wait = false).engine
            logger.info("§aKtor server started on port $apiPort")
        } catch (e: BindException) {
            logger.severe("Failed to start Ktor server. Port $apiPort is likely in use. Please change the port and try again.")
            e.printStackTrace()
            // Disable the plugin
            Bukkit.getPluginManager().disablePlugin(this)
        } catch (e: Exception) {
            logger.severe("An error occurred while starting the Ktor server:")
            e.printStackTrace()
            // Disable the plugin
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }

    private fun Application.module() {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; isLenient = true })
        }

        routing {
            // POST endpoint: Verify token and generate the redeem code, then return it as JSON.
            post("/generateCode") {
                try {
                    // Extract and validate parameters from the POST request body.
                    val params = call.receiveParameters()
                    val token = params["token"] ?: ""
                    if (token != apiToken) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Invalid token"))
                        return@post
                    }

                    val digit = params["digit"]?.toIntOrNull() ?: 5
                    if (digit < 1) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Digit must be at least 1"))
                        return@post
                    }

                    val amount = params["amount"]?.toIntOrNull() ?: 1
                    if (amount < 1) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Amount must be at least 1"))
                        return@post
                    }

                    // Optional parameters.
                    val template = params["template"]
                    val target = params["target"]
                    val targetUUIDParam = params["targetUUID"]

                    // Generate the redeem code using a helper function.
                    val generatedCode = generateRedeemCodeForRequest(digit, amount, template, target, targetUUIDParam)

                    if (generatedCode == null) {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error"))
                    } else {
                        call.respond(mapOf("status" to "ok", "redeemCode" to generatedCode))
                    }
                } catch (e: Exception) {
                    logger.warning("An error occurred in /generateCode route: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal Server Error"))
                }
            }
        }
    }

    /**
     * Helper function that generates the redeem code based on the given parameters.
     *
     * If a template is provided:
     *   - If a target or targetUUID is provided, the codes are modified with that target.
     *   - Otherwise, the codes are generated using the template without modification.
     * If no template is provided, a basic code is generated.
     *
     * @return the generated code(s) as a String, or null if an error occurs.
     */
    private fun generateRedeemCodeForRequest(
        digit: Int, amount: Int, template: String?, target: String?, targetUUIDParam: String?
    ): String? {
        return if (template != null) {
            val codes = api.generateCode(digit, amount, template)

            if (!target.isNullOrBlank() || !targetUUIDParam.isNullOrBlank()) {
                if (!targetUUIDParam.isNullOrBlank()) {
                    logger.info("Received targetUUIDParam: $targetUUIDParam")
                    try {
                        val uuid = UUID.fromString(targetUUIDParam)
                        val targetName = Bukkit.getOfflinePlayer(uuid).name ?: "Unknown"
                        logger.info("Resolved targetName from UUID: $targetName")
                        codes.forEach { code ->
                            logger.info("Modifying code '$code' with targetName: $targetName")
                            api.modifyCode(code, "addTarget", targetName, api.sender)
                        }
                    } catch (e: Exception) {
                        logger.warning("Exception while processing targetUUIDParam: ${e.message}")
                        return null
                    }
                } else if (!target.isNullOrBlank()) {
                    logger.info("Received target: $target")
                    codes.forEach { code ->
                        logger.info("Modifying code '$code' with target: $target")
                        api.modifyCode(code, "addTarget", target, api.sender)
                    }
                }
                // Return the modified code(s)
                if (codes.size == 1) codes.first() else codes.toString()
            } else {
                if (codes.isNotEmpty()) {
                    if (codes.size == 1) codes.first() else codes.toString()
                } else null
            }

        } else {
            // No template provided, generate codes using default logic.
            val codes = api.generateCode(digit, amount, "DEFAULT")
            if (codes.isNotEmpty()) {
                if (codes.size == 1) codes.first() else codes.toString()
            } else null
        }
    }

    // Generates a secure random token.
    private fun generateToken(): String {
        val random = SecureRandom()
        return BigInteger(130, random).toString(32)
    }

    override fun onDisable() {
        ktorServer?.stop(1000, 2000)
    }
}
