package com.envarcade.brennon.bukkit.listener

import com.envarcade.brennon.core.Brennon
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Bukkit listener for player join/quit events.
 *
 * Bridges Bukkit events to the Brennon core player lifecycle.
 */
class BukkitPlayerListener(private val brennon: Brennon) : Listener {

    /**
     * Pre-login: Check for bans before the player fully joins.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        try {
            val uuid = event.uniqueId

            // Check IP ban
            val ip = event.address.hostAddress
            val isIpBanned = try {
                brennon.corePunishmentManager.isIpBanned(ip)
                    .get(5, java.util.concurrent.TimeUnit.SECONDS)
            } catch (e: Throwable) {
                println("[Brennon] IP ban check failed for ${event.name}: ${e.message}")
                false
            }
            if (isIpBanned) {
                event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    "§c§lYou are IP banned from this network."
                )
                return
            }

            val isBanned = brennon.corePunishmentManager.isBanned(uuid)
                .get(5, java.util.concurrent.TimeUnit.SECONDS)

            if (isBanned) {
                val punishments = brennon.corePunishmentManager.getActivePunishments(uuid)
                    .get(5, java.util.concurrent.TimeUnit.SECONDS)
                val ban = punishments.firstOrNull { it.type.name == "BAN" && it.isActive }

                if (ban != null) {
                    val message = buildString {
                        appendLine("§c§lYou are banned from this network.")
                        appendLine()
                        appendLine("§7Reason: §f${ban.reason}")
                        if (ban.isPermanent) {
                            appendLine("§7Duration: §cPermanent")
                        } else {
                            appendLine("§7Expires: §f${ban.expiresAt}")
                        }
                        appendLine()
                        appendLine("§7ID: §f${ban.id}")
                    }
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message)
                }
            }
        } catch (e: Throwable) {
            println("[Brennon] Error during pre-login check for ${event.name}: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            // Allow login on error — don't block players due to DB issues
        }
    }

    /**
     * Player join: Load/create profile, cache player, fire events, load stats.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val ip = player.address?.address?.hostAddress ?: "unknown"

        brennon.corePlayerManager.handleJoin(
            uuid = player.uniqueId,
            name = player.name,
            server = brennon.config.serverName,
            ip = ip
        ).whenComplete { networkPlayer, error ->
            if (error != null) {
                println("[Brennon] Failed to handle join for ${player.name}: ${error.message}")
                return@whenComplete
            }

            // If LuckPerms is active and delegating permissions, sync ranks to LP instead
            if (brennon.luckPermsHook?.isActive == true && brennon.config.luckperms.delegatePermissions) {
                val ranks = networkPlayer.getRanks().map { it.id }.toSet()
                val primaryRank = networkPlayer.getRank().id
                brennon.luckPermsHook?.syncPlayerToLuckPerms(player.uniqueId, ranks, primaryRank)
                return@whenComplete
            }

            // Fallback: Apply rank permissions via Bukkit permission attachment
            val attachment = player.addAttachment(
                player.server.pluginManager.getPlugin("Brennon")!!
            )

            val permissions = networkPlayer.getRanks()
                .flatMap { it.permissions }
                .toSet()

            for (perm in permissions) {
                if (perm.startsWith("-")) {
                    attachment.setPermission(perm.substring(1), false)
                } else {
                    attachment.setPermission(perm, true)
                }
            }

            player.recalculatePermissions()
        }

        // Pre-load stats and increment session counter
        if (brennon.config.modules.stats) {
            brennon.coreStatsManager.handlePlayerJoin(player.uniqueId)
            brennon.coreStatsManager.incrementStat(
                player.uniqueId,
                com.envarcade.brennon.api.stats.StatTypes.SESSIONS,
                1.0
            )
        }
    }

    /**
     * Player quit: Persist data, remove from cache, flush stats, cleanup chat.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId

        brennon.corePlayerManager.handleQuit(uuid).whenComplete { _, error ->
            if (error != null) {
                println("[Brennon] Failed to handle quit for ${event.player.name}: ${error.message}")
            }
        }

        // Flush stats to DB
        if (brennon.config.modules.stats) {
            brennon.coreStatsManager.handlePlayerQuit(uuid)
        }

        // Cleanup chat state
        if (brennon.config.modules.chat) {
            brennon.coreChatManager.handlePlayerQuit(uuid)
        }
    }
}
