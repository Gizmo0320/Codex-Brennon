package com.envarcade.brennon.core.chat

import com.envarcade.brennon.api.chat.ChatChannel
import com.envarcade.brennon.api.chat.ChatFilter
import com.envarcade.brennon.api.chat.ChatManager
import com.envarcade.brennon.common.config.BrennonConfig
import com.envarcade.brennon.core.event.ChatMessageEvent
import com.envarcade.brennon.core.event.ChannelSwitchEvent
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.PrivateMessageEvent
import com.envarcade.brennon.core.player.CorePlayerManager
import com.envarcade.brennon.core.punishment.CorePunishmentManager
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.packet.ChatMessagePacket
import com.envarcade.brennon.messaging.packet.Packet
import com.envarcade.brennon.messaging.packet.PrivateMessagePacket
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CoreChatManager(
    private val playerManager: CorePlayerManager,
    private val punishmentManager: CorePunishmentManager,
    private val messaging: RedisMessagingService,
    private val eventBus: CoreEventBus,
    private val config: BrennonConfig,
    private val chatNetworkId: String? = null
) : ChatManager {

    private val channels = ConcurrentHashMap<String, CoreChatChannel>()
    private val filters = mutableListOf<CoreChatFilter>()
    private val playerChannels = ConcurrentHashMap<UUID, String>()
    private val playerSubscriptions = ConcurrentHashMap<UUID, MutableSet<String>>()
    val lastMessageRecipients = ConcurrentHashMap<UUID, UUID>()

    /** Platform hook — delivers a message component to a local player. */
    var localMessageSender: ((UUID, net.kyori.adventure.text.Component) -> Unit)? = null

    /** Optional stats tracking — set by Brennon bootstrap when stats module is enabled. */
    var statsTracker: ((UUID, String) -> Unit)? = null

    fun initialize() {
        // Load channels from config
        for (channelData in config.chat.channels) {
            channels[channelData.id] = CoreChatChannel(channelData)
        }

        // Load filters from config
        for (filterData in config.chat.filters) {
            filters.add(CoreChatFilter(filterData))
        }

        // Subscribe to cross-server chat messages (network-scoped if configured)
        messaging.subscribe(Channels.chatMessage(chatNetworkId)) { _, message ->
            try {
                val packet = Packet.deserialize(message, ChatMessagePacket::class.java)
                if (packet.server != config.serverName) {
                    deliverLocalMessage(packet.channelId, packet.senderName, packet.message)
                }
            } catch (e: Exception) {
                println("[Brennon] Error receiving chat message: ${e.message}")
            }
        }

        // Subscribe to private messages (network-scoped if configured)
        messaging.subscribe(Channels.chatPrivate(chatNetworkId)) { _, message ->
            try {
                val packet = Packet.deserialize(message, PrivateMessagePacket::class.java)
                val recipientUuid = UUID.fromString(packet.recipientUuid)
                if (playerManager.isOnline(recipientUuid)) {
                    val formatted = com.envarcade.brennon.common.util.TextUtil.parse(
                        "<gray>[<gold>${packet.senderName}</gold> <dark_gray>\u2192 <gold>You</gold>] <white>${packet.message}"
                    )
                    localMessageSender?.invoke(recipientUuid, formatted)
                    lastMessageRecipients[recipientUuid] = UUID.fromString(packet.senderUuid)
                }
            } catch (e: Exception) {
                println("[Brennon] Error receiving private message: ${e.message}")
            }
        }

        println("[Brennon] Chat manager initialized with ${channels.size} channels and ${filters.size} filters.")
    }

    fun shutdown() {
        messaging.unsubscribe(Channels.chatMessage(chatNetworkId))
        messaging.unsubscribe(Channels.chatPrivate(chatNetworkId))
        playerChannels.clear()
        playerSubscriptions.clear()
        lastMessageRecipients.clear()
    }

    override fun getChannel(id: String): Optional<ChatChannel> {
        return Optional.ofNullable(channels[id])
    }

    override fun getChannels(): Collection<ChatChannel> {
        return channels.values.toList()
    }

    override fun getDefaultChannel(): ChatChannel {
        return channels.values.firstOrNull { it.isDefault }
            ?: channels.values.first()
    }

    override fun getPlayerChannel(player: UUID): ChatChannel {
        val channelId = playerChannels[player] ?: config.chat.defaultChannel
        return channels[channelId] ?: getDefaultChannel()
    }

    override fun setPlayerChannel(player: UUID, channelId: String) {
        val oldChannelId = playerChannels[player] ?: config.chat.defaultChannel
        val channel = channels[channelId] ?: return
        playerChannels[player] = channelId
        eventBus.publish(ChannelSwitchEvent(player, oldChannelId, channelId))
    }

    override fun sendMessage(sender: UUID, senderName: String, channelId: String, message: String) {
        val channel = channels[channelId] ?: return

        // Check if muted
        if (isMutedInChannel(sender, channelId)) return

        // Apply filters
        var processedMessage = message
        for (filter in filters) {
            if (!filter.isEnabled) continue
            for (pattern in filter.patterns) {
                if (pattern.matcher(processedMessage).find()) {
                    when (filter.action) {
                        ChatFilter.FilterAction.BLOCK -> return
                        ChatFilter.FilterAction.CENSOR -> {
                            processedMessage = pattern.matcher(processedMessage).replaceAll(filter.replacement)
                        }
                        ChatFilter.FilterAction.FLAG -> {
                            messaging.publish(Channels.STAFF_ALERT, "[ChatFilter] ${filter.id} flagged message from $senderName: $message")
                        }
                        ChatFilter.FilterAction.LOG -> {
                            println("[Brennon] [ChatFilter] ${filter.id} logged message from $senderName: $message")
                        }
                    }
                }
            }
        }

        // Publish event
        val event = ChatMessageEvent(sender, senderName, channelId, processedMessage, config.serverName)
        eventBus.publish(event)
        if (event.isCancelled) return

        // Track stat
        statsTracker?.invoke(sender, com.envarcade.brennon.api.stats.StatTypes.MESSAGES_SENT)

        // Deliver locally
        deliverLocalMessage(channelId, senderName, processedMessage)

        // Broadcast cross-server if needed
        if (channel.isCrossServer) {
            val packet = ChatMessagePacket(
                senderUuid = sender.toString(),
                senderName = senderName,
                channelId = channelId,
                message = processedMessage,
                server = config.serverName
            )
            messaging.publish(Channels.chatMessage(chatNetworkId), Packet.serialize(packet))
        }
    }

    override fun sendPrivateMessage(sender: UUID, senderName: String, recipient: UUID, message: String) {
        // Publish event
        val event = PrivateMessageEvent(sender, senderName, recipient, message)
        eventBus.publish(event)
        if (event.isCancelled) return

        // Track for /reply
        lastMessageRecipients[recipient] = sender
        lastMessageRecipients[sender] = recipient

        // Send to sender
        val senderFormatted = com.envarcade.brennon.common.util.TextUtil.parse(
            "<gray>[<gold>You</gold> <dark_gray>\u2192 <gold>${playerManager.getCachedPlayer(recipient)?.name ?: "Unknown"}</gold>] <white>$message"
        )
        localMessageSender?.invoke(sender, senderFormatted)

        // Send via Redis (recipient may be on another server)
        val packet = PrivateMessagePacket(
            senderUuid = sender.toString(),
            senderName = senderName,
            recipientUuid = recipient.toString(),
            message = message
        )
        messaging.publish(Channels.chatPrivate(chatNetworkId), Packet.serialize(packet))
    }

    override fun isMutedInChannel(player: UUID, channelId: String): Boolean {
        return try {
            punishmentManager.isMuted(player).join()
        } catch (e: Exception) {
            false
        }
    }

    override fun toggleChannelSubscription(player: UUID, channelId: String) {
        val subs = playerSubscriptions.computeIfAbsent(player) { mutableSetOf(config.chat.defaultChannel) }
        if (subs.contains(channelId)) {
            subs.remove(channelId)
        } else {
            subs.add(channelId)
        }
    }

    fun handlePlayerQuit(uuid: UUID) {
        playerChannels.remove(uuid)
        playerSubscriptions.remove(uuid)
        lastMessageRecipients.remove(uuid)
    }

    private fun deliverLocalMessage(channelId: String, senderName: String, message: String) {
        val channel = channels[channelId] ?: return
        val formatted = com.envarcade.brennon.common.util.TextUtil.parse(
            channel.format
                .replace("<player>", senderName)
                .replace("<message>", message)
        )
        // Deliver to all online players subscribed to this channel
        for (player in playerManager.getOnlinePlayers()) {
            val uuid = player.uniqueId
            val permission = channel.permission
            if (permission.isNotEmpty() && !player.hasPermission(permission)) continue
            localMessageSender?.invoke(uuid, formatted)
        }
    }
}
