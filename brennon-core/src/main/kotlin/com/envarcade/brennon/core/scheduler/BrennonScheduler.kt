package com.envarcade.brennon.core.scheduler

import com.envarcade.brennon.api.stats.StatTypes
import com.envarcade.brennon.core.player.CorePlayerManager
import com.envarcade.brennon.core.stats.CoreStatsManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Handles periodic tasks for the Brennon core:
 * - Auto-saving player data
 * - Refreshing Redis session TTLs
 * - Flushing cached stats to DB
 * - Tracking playtime for online players
 */
class BrennonScheduler(
    private val playerManager: CorePlayerManager,
    private val statsManager: CoreStatsManager?
) {

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { runnable ->
        Thread(runnable, "Brennon-Scheduler").apply { isDaemon = true }
    }

    /**
     * Starts all scheduled tasks.
     */
    fun start() {
        // Auto-save player data every 5 minutes
        executor.scheduleAtFixedRate({
            try {
                val count = playerManager.getOnlinePlayers().size
                if (count > 0) {
                    playerManager.saveAll().whenComplete { _, error ->
                        if (error != null) {
                            println("[Brennon] Auto-save failed: ${error.message}")
                        } else {
                            println("[Brennon] Auto-saved $count player(s).")
                        }
                    }
                }
            } catch (e: Exception) {
                println("[Brennon] Auto-save error: ${e.message}")
            }
        }, 5, 5, TimeUnit.MINUTES)

        // Refresh Redis session TTLs every 2 minutes
        executor.scheduleAtFixedRate({
            try {
                playerManager.refreshAllSessions()
            } catch (e: Exception) {
                println("[Brennon] Session refresh error: ${e.message}")
            }
        }, 2, 2, TimeUnit.MINUTES)

        // Flush cached stats to DB every 5 minutes
        if (statsManager != null) {
            executor.scheduleAtFixedRate({
                try {
                    statsManager.flushAll()
                } catch (e: Exception) {
                    println("[Brennon] Stats flush error: ${e.message}")
                }
            }, 5, 5, TimeUnit.MINUTES)

            // Increment playtime for all online players every 1 minute
            executor.scheduleAtFixedRate({
                try {
                    for (player in playerManager.getOnlinePlayers()) {
                        statsManager.incrementStat(player.uniqueId, StatTypes.PLAYTIME, 60.0)
                    }
                } catch (e: Exception) {
                    println("[Brennon] Playtime tracking error: ${e.message}")
                }
            }, 1, 1, TimeUnit.MINUTES)
        }

        val tasks = mutableListOf("auto-save: 5min", "session refresh: 2min")
        if (statsManager != null) {
            tasks.add("stats flush: 5min")
            tasks.add("playtime: 1min")
        }
        println("[Brennon] Scheduler started (${tasks.joinToString(", ")})")
    }

    /**
     * Stops all scheduled tasks.
     */
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
