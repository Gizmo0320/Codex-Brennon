package com.envarcade.brennon.discord

import com.envarcade.brennon.common.config.DiscordConfig
import com.envarcade.brennon.core.Brennon
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.UUID

/**
 * Listens for Discord messages and relays them to Minecraft.
 *
 * Messages sent in the configured chat channel are forwarded to
 * the global Minecraft chat channel via CoreChatManager.
 */
class DiscordListener(
    private val brennon: Brennon,
    private val config: DiscordConfig
) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        // Ignore bots and self
        if (event.author.isBot) return
        if (event.author.isSystem) return

        val channelId = event.channel.id

        // Chat channel relay: Discord -> Minecraft
        if (channelId == config.chatChannelId && config.syncChat) {
            if (!brennon.config.modules.chat) return

            val discordName = event.member?.effectiveName ?: event.author.name
            val message = event.message.contentStripped

            if (message.isBlank()) return

            // Relay to global chat channel as a special Discord sender
            brennon.coreChatManager.sendMessage(
                DISCORD_UUID,
                "[Discord] $discordName",
                "global",
                message
            )
        }
    }

    companion object {
        /** Synthetic UUID representing Discord senders. */
        val DISCORD_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
