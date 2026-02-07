package com.envarcade.brennon.proxy.listener

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Velocity proxy listener for player lifecycle events.
 *
 * Handles login (ban checks), disconnect, and server switch events
 * on the proxy layer for network-wide tracking.
 */
class ProxyPlayerListener(
    private val brennon: Brennon,
    private val proxy: ProxyServer
) {

    /**
     * Login: Check for bans and load player data.
     */
    @Subscribe(order = PostOrder.EARLY)
    fun onLogin(event: LoginEvent) {
        val player = event.player
        val uuid = player.uniqueId

        try {
            val isBanned = brennon.corePunishmentManager.isBanned(uuid).join()

            if (isBanned) {
                val punishments = brennon.corePunishmentManager.getActivePunishments(uuid).join()
                val ban = punishments.firstOrNull { it.type.name == "BAN" && it.isActive }

                if (ban != null) {
                    val message = Component.empty()
                        .append(Component.text("You are banned from this network.", NamedTextColor.RED))
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(Component.text("Reason: ", NamedTextColor.GRAY))
                        .append(Component.text(ban.reason, NamedTextColor.WHITE))
                        .append(Component.newline())
                        .append(
                            if (ban.isPermanent) {
                                Component.text("Duration: ", NamedTextColor.GRAY)
                                    .append(Component.text("Permanent", NamedTextColor.RED))
                            } else {
                                Component.text("Expires: ", NamedTextColor.GRAY)
                                    .append(Component.text(ban.expiresAt.toString(), NamedTextColor.WHITE))
                            }
                        )
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(Component.text("ID: ", NamedTextColor.GRAY))
                        .append(Component.text(ban.id, NamedTextColor.WHITE))

                    event.result = ResultedEvent.ComponentResult.denied(message)
                    return
                }
            }

            // Pre-load player data on the proxy
            val ip = player.remoteAddress.address.hostAddress
            brennon.corePlayerManager.handleJoin(uuid, player.username, "proxy", ip).join()

        } catch (e: Exception) {
            println("[Brennon] Error during proxy login for ${player.username}: ${e.message}")
            // Allow login on error
        }
    }

    /**
     * Server connected: Track server switches.
     */
    @Subscribe(order = PostOrder.LAST)
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        val newServer = event.server.serverInfo.name
        val previousServer = event.previousServer.orElse(null)?.serverInfo?.name

        brennon.corePlayerManager.handleServerSwitch(player.uniqueId, newServer)

        if (previousServer != null) {
            println("[Brennon] ${player.username} switched: $previousServer -> $newServer")
        }
    }

    /**
     * Disconnect: Persist data, flush stats, cleanup chat.
     */
    @Subscribe(order = PostOrder.LATE)
    fun onDisconnect(event: DisconnectEvent) {
        val uuid = event.player.uniqueId

        brennon.corePlayerManager.handleQuit(uuid).whenComplete { _, error ->
            if (error != null) {
                println("[Brennon] Error during disconnect for ${event.player.username}: ${error.message}")
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
