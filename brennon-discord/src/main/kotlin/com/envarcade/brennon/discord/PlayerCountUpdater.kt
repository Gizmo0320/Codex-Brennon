package com.envarcade.brennon.discord

import com.envarcade.brennon.core.Brennon
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Updates the Discord bot's activity status with the current
 * network player count every 30 seconds.
 */
class PlayerCountUpdater(
    private val brennon: Brennon,
    private val jda: JDA
) {

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Brennon-DiscordActivity").apply { isDaemon = true }
    }

    fun start() {
        executor.scheduleAtFixedRate({
            try {
                val count = brennon.coreServerManager.getNetworkPlayerCount()
                val label = if (count == 1) "player" else "players"
                jda.presence.activity = Activity.watching("$count $label online")
            } catch (e: Exception) {
                println("[Brennon] Error updating Discord activity: ${e.message}")
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

    fun stop() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            executor.shutdownNow()
        }
    }
}
