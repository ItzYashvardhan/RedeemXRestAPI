package me.justlime.redeemXRestAPI.configuration

import me.justlime.redeemXRestAPI.enums.JConfig
import me.justlime.redeemXRestAPI.enums.JFiles
import me.justlime.redeemXRestAPI.rxrPlugin
import me.justlime.redeemXRestAPI.utilities.JService
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Level

class ConfigManager {
    lateinit var configuration: FileConfiguration
    lateinit var messages: FileConfiguration
    lateinit var endpoints: FileConfiguration

    init {
        loadFileConfiguration()
    }

    fun loadFileConfiguration() {
        rxrPlugin.saveDefaultConfig()
        configuration = getConfig(JFiles.CONFIG)
        messages = getConfig(JFiles.MESSAGES)
        loadToken()
    }

    private fun getFile(configFile: JFiles): File {
        return if (configFile == JFiles.MESSAGES) {
            val lang = getConfig(JFiles.CONFIG).getString("lang", "en") ?: "en"
            val filename = configFile.fileName.replace("{lang}", lang)
            File(rxrPlugin.dataFolder, filename)
        } else {
            File(rxrPlugin.dataFolder, configFile.fileName)
        }
    }

    private fun getConfig(configFile: JFiles): FileConfiguration {
        if (!rxrPlugin.dataFolder.exists()) rxrPlugin.dataFolder.mkdir()
        val file = getFile(configFile)
        if (!file.exists()) {
            rxrPlugin.logger.log(
                Level.WARNING,
                "File not found: ${file.name}. Falling back to default or generating new."
            )
            if (configFile == JFiles.MESSAGES) {
                // Fallback to default language (en)
                val defaultFile = File(rxrPlugin.dataFolder, configFile.fileName.replace("{lang}", "en"))
                if (!defaultFile.exists()) {
                    rxrPlugin.saveResource("messages_en.yml", false)
                }
                return YamlConfiguration.loadConfiguration(defaultFile)
            } else {
                rxrPlugin.saveResource(configFile.fileName, false)
            }
        }
        return YamlConfiguration.loadConfiguration(file)
    }

    private fun loadToken() {
        var apiToken = configuration.getString(JConfig.API_TOKEN.path) ?: ""
        if (apiToken.isBlank()) {
            apiToken = JService.generateToken()
            configuration.set("api.token", apiToken)
        }
        saveConfig(JFiles.CONFIG)
    }

    fun saveConfig(configFile: JFiles): Boolean {
        try {
            when (configFile) {
                JFiles.CONFIG -> configuration
                JFiles.MESSAGES -> messages
                JFiles.ENDPOINTS -> endpoints
            }.save(File(rxrPlugin.dataFolder, configFile.fileName))
            return true
        } catch (e: Exception) {
            rxrPlugin.logger.log(Level.SEVERE, "Could not save ${configFile.fileName}: ${e.message}")
            return false
        }
    }

    fun reload() {
        loadFileConfiguration()
    }


}