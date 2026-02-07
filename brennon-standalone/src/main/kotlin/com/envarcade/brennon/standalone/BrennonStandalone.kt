package com.envarcade.brennon.standalone

import com.envarcade.brennon.api.Platform
import com.envarcade.brennon.core.Brennon
import java.io.File
import java.util.Scanner

/**
 * Brennon Network Core — Standalone Application.
 *
 * Used for running Brennon services outside of Minecraft
 * (e.g., Discord bots, REST APIs, analytics, etc.)
 */
fun main(args: Array<String>) {
    println()
    println("  ____                                    ")
    println(" | __ ) _ __ ___ _ __  _ __   ___  _ __   ")
    println(" |  _ \\| '__/ _ \\ '_ \\| '_ \\ / _ \\| '_ \\ ")
    println(" | |_) | | |  __/ | | | | | | (_) | | | | ")
    println(" |____/|_|  \\___|_| |_|_| |_|\\___/|_| |_| ")
    println()
    println("  Network Core — Standalone Mode")
    println()

    val dataFolder = File(args.firstOrNull() ?: "data")
    dataFolder.mkdirs()

    val brennon = Brennon(Platform.STANDALONE, dataFolder)

    // Shutdown hook for Ctrl+C
    Runtime.getRuntime().addShutdownHook(Thread {
        brennon.disable()
    })

    try {
        brennon.enable()
    } catch (e: Exception) {
        println("[Brennon] FATAL: Failed to start: ${e.message}")
        e.printStackTrace()
        return
    }

    println()
    println("[Brennon] Standalone mode active. Type 'stop' to shut down.")
    println()

    // Simple console command loop
    val scanner = Scanner(System.`in`)
    while (scanner.hasNextLine()) {
        val line = scanner.nextLine().trim().lowercase()
        when (line) {
            "stop", "quit", "exit" -> {
                brennon.disable()
                break
            }
            "status" -> {
                println("[Brennon] Status: Running")
                println("[Brennon] Platform: ${brennon.getPlatform().displayName}")
                println("[Brennon] Database: ${if (brennon.databaseManager.isConnected()) "Connected" else "Disconnected"}")
                println("[Brennon] Redis: ${if (brennon.redisMessaging.isConnected()) "Connected" else "Disconnected"}")
            }
            "help" -> {
                println("[Brennon] Commands: stop, status, help")
            }
            else -> {
                if (line.isNotEmpty()) {
                    println("[Brennon] Unknown command: $line (type 'help' for commands)")
                }
            }
        }
    }
}
