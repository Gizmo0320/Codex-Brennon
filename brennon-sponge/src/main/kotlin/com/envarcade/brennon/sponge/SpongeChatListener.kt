package com.envarcade.brennon.sponge

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.message.PlayerChatEvent

/**
 * Sponge listener for chat events.
 *
 * Routes chat through CoreChatManager when enabled,
 * otherwise falls back to basic mute enforcement.
 */
class SpongeChatListener(private val brennon: Brennon) {

    @Listener(order = Order.EARLY)
    fun onChat(event: PlayerChatEvent) {
        val player = event.cause().first(org.spongepowered.api.entity.living.player.server.ServerPlayer::class.java).orElse(null) ?: return
        val uuid = player.uniqueId()
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())

        if (brennon.config.modules.chat) {
            event.setCancelled(true)
            val channel = brennon.coreChatManager.getPlayerChannel(uuid)
            val networkPlayer = brennon.corePlayerManager.getCachedPlayer(uuid)
            val senderName = networkPlayer?.name ?: player.name()
            brennon.coreChatManager.sendMessage(uuid, senderName, channel.id, message)
            return
        }

        // Fallback: basic mute check
        try {
            val isMuted = brennon.corePunishmentManager.isMuted(uuid).join()
            if (isMuted) {
                event.setCancelled(true)
                player.sendMessage(TextUtil.error("You are muted and cannot chat."))
            }
        } catch (e: Exception) {
            println("[Brennon] Error checking mute for ${player.name()}: ${e.message}")
        }
    }
}
