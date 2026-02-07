package com.envarcade.brennon.bukkit.listener

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/**
 * Bukkit listener for chat events.
 *
 * Routes chat through the Brennon chat manager when the chat module
 * is enabled, otherwise falls back to basic mute check + rank formatting.
 */
class BukkitChatListener(private val brennon: Brennon) : Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())

        // If the chat module is active, route through CoreChatManager
        if (brennon.config.modules.chat) {
            event.isCancelled = true
            val channel = brennon.coreChatManager.getPlayerChannel(uuid)
            val networkPlayer = brennon.corePlayerManager.getCachedPlayer(uuid)
            val senderName = networkPlayer?.name ?: player.name
            brennon.coreChatManager.sendMessage(uuid, senderName, channel.id, message)
            return
        }

        // Fallback: basic mute check + rank formatting
        try {
            val isMuted = brennon.corePunishmentManager.isMuted(uuid).join()
            if (isMuted) {
                event.isCancelled = true
                player.sendMessage(TextUtil.error("You are muted and cannot chat."))
                return
            }
        } catch (e: Exception) {
            println("[Brennon] Error checking mute for ${player.name}: ${e.message}")
        }

        // Format chat with rank prefix
        val networkPlayer = brennon.corePlayerManager.getCachedPlayer(uuid)
        if (networkPlayer != null) {
            val displayName = networkPlayer.displayName

            event.renderer { _, _, msg, _ ->
                Component.empty()
                    .append(displayName)
                    .append(Component.text(" "))
                    .append(Component.text("» ", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY))
                    .append(Component.text(
                        PlainTextComponentSerializer.plainText().serialize(msg),
                        net.kyori.adventure.text.format.NamedTextColor.WHITE
                    ))
            }
        }
    }
}
