package com.envarcade.brennon.common.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object ConfigLoader {

    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    fun <T> load(file: File, clazz: Class<T>, default: T): T {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            save(file, default)
            return default
        }

        return try {
            FileReader(file).use { reader ->
                gson.fromJson(reader, clazz)
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to load config ${file.name}: ${e.message}")
            println("[Brennon] Using default configuration.")
            default
        }
    }

    fun <T> save(file: File, config: T) {
        file.parentFile?.mkdirs()
        FileWriter(file).use { writer ->
            gson.toJson(config, writer)
        }
    }

    fun loadBrennonConfig(dataFolder: File): BrennonConfig {
        val configFile = File(dataFolder, "config.json")
        return load(configFile, BrennonConfig::class.java, BrennonConfig())
    }
}
