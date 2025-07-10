package me.justlime.redeemXRestAPI.utilities

import me.justlime.redeemXRestAPI.configuration.ConfigManager
import org.bukkit.configuration.file.FileConfiguration
import java.math.BigInteger
import java.security.SecureRandom

object JService {

    lateinit var configManager: ConfigManager
    lateinit var config: FileConfiguration


    fun init(){
        configManager = ConfigManager()
        configManager.loadFileConfiguration()
        config = configManager.configuration

    }
    fun generateToken(): String {
        val random = SecureRandom()
        return BigInteger(130, random).toString(32)
    }

}