package com.envarcade.brennon.discord

import com.envarcade.brennon.common.config.DiscordConfig
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.packet.ChatMessagePacket
import com.envarcade.brennon.messaging.packet.Packet
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

/**
 * Relays Minecraft events to Discord channels.
 *
 * Subscribes to Redis pub/sub channels and forwards:
 * - Chat messages to the chat channel
 * - Punishments to the staff channel
 * - Staff alerts to the alert channel
 */
class MinecraftChatRelay(
    private val brennon: Brennon,
    private val jda: JDA,
    private val config: DiscordConfig
) {

    fun initialize() {
        // Relay Minecraft chat -> Discord
        if (config.syncChat && config.chatChannelId.isNotEmpty()) {
            brennon.redisMessaging.subscribe(Channels.CHAT_MESSAGE) { _, message ->
                try {
                    val packet = Packet.deserialize(message, ChatMessagePacket::class.java)
                    // Don't relay messages that came from Discord
                    if (packet.senderUuid == DiscordListener.DISCORD_UUID.toString()) return@subscribe
                    val channel = getChatChannel() ?: return@subscribe
                    channel.sendMessage("**${packet.senderName}** [${packet.server}]: ${packet.message}").queue()
                } catch (e: Exception) {
                    println("[Brennon] Error relaying chat to Discord: ${e.message}")
                }
            }
        }

        // Relay punishments -> Discord staff channel
        if (config.syncPunishments && config.staffChannelId.isNotEmpty()) {
            brennon.redisMessaging.subscribe(Channels.PUNISHMENT_ISSUED) { _, message ->
                try {
                    val staffChannel = getStaffChannel() ?: return@subscribe
                    staffChannel.sendMessage("\uD83D\uDD28 $message").queue()
                } catch (e: Exception) {
                    println("[Brennon] Error relaying punishment to Discord: ${e.message}")
                }
            }
        }

        // Relay staff alerts -> Discord alert channel
        if (config.alertChannelId.isNotEmpty()) {
            brennon.redisMessaging.subscribe(Channels.STAFF_ALERT) { _, message ->
                try {
                    val alertChannel = getAlertChannel() ?: return@subscribe
                    alertChannel.sendMessage("\u26A0\uFE0F $message").queue()
                } catch (e: Exception) {
                    println("[Brennon] Error relaying alert to Discord: ${e.message}")
                }
            }
        }

        // Relay player join/quit
        if (config.syncChat && config.chatChannelId.isNotEmpty()) {
            brennon.redisMessaging.subscribe(Channels.PLAYER_JOIN) { _, message ->
                try {
                    val channel = getChatChannel() ?: return@subscribe
                    channel.sendMessage("\u2705 $message joined the network").queue()
                } catch (_: Exception) { }
            }
            brennon.redisMessaging.subscribe(Channels.PLAYER_QUIT) { _, message ->
                try {
                    val channel = getChatChannel() ?: return@subscribe
                    channel.sendMessage("\u274C $message left the network").queue()
                } catch (_: Exception) { }
            }
        }

        println("[Brennon] Minecraft -> Discord relay initialized.")
    }

    fun shutdown() {
        // Unsubscribe is handled by the messaging service shutdown
    }

    private fun getChatChannel(): TextChannel? =
        jda.getTextChannelById(config.chatChannelId)

    private fun getStaffChannel(): TextChannel? =
        jda.getTextChannelById(config.staffChannelId)

    private fun getAlertChannel(): TextChannel? =
        jda.getTextChannelById(config.alertChannelId)
}
