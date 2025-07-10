package me.justlime.redeemXRestAPI

import api.justlime.redeemcodex.RedeemXAPI
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.justlime.redeemXRestAPI.route.routeManager
import me.justlime.redeemXRestAPI.utilities.JService
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.net.BindException
import java.util.*


lateinit var rxrPlugin: RedeemXRestAPI

class RedeemXRestAPI : JavaPlugin() {
    private var ktorServer: NettyApplicationEngine? = null

    override fun onEnable() {
        rxrPlugin = this
        saveDefaultConfig()
        JService.init()
        val apiEnabled = JService.config.getBoolean("api.enabled", false)
        logger.info("RedeemXRestAPI API Enabled: $apiEnabled")
        if (apiEnabled) {
            startRestApi()
        }
    }

    private fun startRestApi() {
        val apiPort = JService.config.getInt("port", 8080)
        val apiHost = JService.config.getString("host")?.takeIf { it.isNotBlank() } ?: "0.0.0.0"
        try {
            ktorServer = embeddedServer(Netty, port = apiPort, host = apiHost) { routeManager() }.start(wait = false).engine
            logger.info("§aKtor server started on port $apiPort")
        } catch (e: BindException) {
            logger.severe("Failed to start Ktor server. Port $apiPort is likely in use. Please change the port and try again.")
            Bukkit.getPluginManager().disablePlugin(this)
        } catch (e: Exception) {
            logger.severe("An error occurred while starting the Ktor server:")
            e.printStackTrace()
            Bukkit.getPluginManager().disablePlugin(this)
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
        return (if (template != null) {
            val codes = RedeemXAPI.code.generateCode(digit, template, amount)

            if (!target.isNullOrBlank() || !targetUUIDParam.isNullOrBlank()) {
                if (!targetUUIDParam.isNullOrBlank()) {
                    logger.info("Received targetUUIDParam: $targetUUIDParam")
                    try {
                        val uuid = UUID.fromString(targetUUIDParam)
                        val targetName = Bukkit.getOfflinePlayer(uuid).name ?: "Unknown"
                        logger.info("Resolved targetName from UUID: $targetName")
                        codes.forEach { code ->
                            logger.info("Modifying code '$code' with targetName: $targetName")
                            code.target.add(targetName)
                        }
                        RedeemXAPI.code.upsertCodes(codes.toList())

                    } catch (e: Exception) {
                        logger.warning("Exception while processing targetUUIDParam: ${e.message}")
                        return null
                    }
                } else if (!target.isNullOrBlank()) {
                    logger.info("Received target: $target")
                    codes.forEach { code ->
                        logger.info("Modifying code '$code' with target: $target")
                        code.target.add(target)
                        RedeemXAPI.code.upsertCode(code)
                    }
                }
                // Return the modified code(s)
                if (codes.size == 1) codes.first() else codes.toString()
            } else {
                if (codes.isNotEmpty()) {
                    if (codes.size == 1) codes.first() else codes.toString()
                } else null
            }
            RedeemXAPI.code.upsertCodes(codes.toList())


        } else {
            // No template provided, generate codes using default logic.
            val codes = RedeemXAPI.code.generateCode(digit, "DEFAULT", amount)
            if (codes.isNotEmpty()) {
                if (codes.size == 1) codes.first() else codes.toString()
            } else null
            RedeemXAPI.code.upsertCodes(codes.toList())
        }) as String?
    }

    // Generates a secure random token.

    override fun onDisable() {
        ktorServer?.stop(1000, 2000)
    }
}
