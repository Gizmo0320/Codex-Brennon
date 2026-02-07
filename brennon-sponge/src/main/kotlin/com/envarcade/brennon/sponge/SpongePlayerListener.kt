package com.envarcade.brennon.sponge

import com.envarcade.brennon.core.Brennon
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.network.ServerSideConnectionEvent

/**
 * Sponge listener for player join/quit events.
 */
class SpongePlayerListener(private val brennon: Brennon) {

    @Listener(order = Order.EARLY)
    fun onLogin(event: ServerSideConnectionEvent.Login) {
        val profile = event.profile()
        val uuid = profile.uniqueId()

        // Check for bans
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

                    event.setMessage(message)
                    event.setCancelled(true)
                    return
                }
            }
        } catch (e: Exception) {
            println("[Brennon] Error during login check for ${profile.name().orElse("unknown")}: ${e.message}")
        }
    }

    @Listener(order = Order.LAST)
    fun onJoin(event: ServerSideConnectionEvent.Join) {
        val player = event.player()
        val uuid = player.uniqueId()
        val name = player.name()
        val ip = player.connection().address().address.hostAddress

        brennon.corePlayerManager.handleJoin(
            uuid = uuid,
            name = name,
            server = brennon.config.serverName,
            ip = ip
        ).whenComplete { _, error ->
            if (error != null) {
                println("[Brennon] Failed to handle join for $name: ${error.message}")
            }
        }

        // Pre-load stats
        if (brennon.config.modules.stats) {
            brennon.coreStatsManager.handlePlayerJoin(uuid)
        }
    }

    @Listener(order = Order.LAST)
    fun onDisconnect(event: ServerSideConnectionEvent.Disconnect) {
        val uuid = event.player().uniqueId()

        brennon.corePlayerManager.handleQuit(uuid).whenComplete { _, error ->
            if (error != null) {
                println("[Brennon] Failed to handle quit for ${event.player().name()}: ${error.message}")
            }
        }

        if (brennon.config.modules.stats) {
            brennon.coreStatsManager.handlePlayerQuit(uuid)
        }
        if (brennon.config.modules.chat) {
            brennon.coreChatManager.handlePlayerQuit(uuid)
        }
    }
}
