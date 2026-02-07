package com.envarcade.brennon.proxy.listener

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.proxy.ProxyServer

/**
 * Velocity proxy listener for chat events.
 *
 * When the chat module is enabled, routes all chat through
 * CoreChatManager for mute enforcement, filtering, and
 * cross-server channel delivery.
 */
class ProxyChatListener(
    private val brennon: Brennon,
    private val proxy: ProxyServer
) {

    @Subscribe(order = PostOrder.EARLY)
    fun onChat(event: PlayerChatEvent) {
        if (!brennon.config.modules.chat) return

        val player = event.player
        val uuid = player.uniqueId

        // Mute check — notify the player but do NOT cancel/deny the event.
        // Since 1.19.1, denying signed chat messages on the proxy causes
        // "illegal protocol state" disconnections. The backend server
        // (Bukkit/Folia) handles the actual chat cancellation safely via
        // AsyncChatEvent.isCancelled.
        try {
            val isMuted = brennon.corePunishmentManager.isMuted(uuid).join()
            if (isMuted) {
                player.sendMessage(TextUtil.error("You are muted and cannot chat."))
                return
            }
        } catch (e: Exception) {
            println("[Brennon] Error checking mute for ${player.username}: ${e.message}")
        }

        // Chat routing is handled by the backend server's chat listener
        // (BukkitChatListener / FoliaChatListener). The proxy does NOT
        // cancel or re-route the message to avoid signed chat violations.
    }
}
