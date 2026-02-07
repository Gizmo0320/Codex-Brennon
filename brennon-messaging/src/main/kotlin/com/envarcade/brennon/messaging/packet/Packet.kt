package com.envarcade.brennon.messaging.packet

import com.google.gson.Gson
import java.time.Instant
import java.util.UUID

abstract class Packet(
    val packetId: String = UUID.randomUUID().toString().substring(0, 8),
    val sourceServer: String = "",
    val timestamp: Long = Instant.now().toEpochMilli(),
    val networkId: String? = null
) {
    companion object {
        private val gson = Gson()

        fun <T : Packet> serialize(packet: T): String = gson.toJson(packet)

        fun <T : Packet> deserialize(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)
    }
}

data class PlayerJoinPacket(
    val playerUuid: String,
    val playerName: String,
    val server: String
) : Packet()

data class PlayerQuitPacket(
    val playerUuid: String,
    val playerName: String,
    val server: String
) : Packet()

data class PlayerSwitchPacket(
    val playerUuid: String,
    val playerName: String,
    val fromServer: String,
    val toServer: String
) : Packet()

data class PunishmentPacket(
    val punishmentId: String,
    val targetUuid: String,
    val type: String,
    val reason: String,
    val duration: Long?
) : Packet()

data class BroadcastPacket(
    val message: String,
    val permission: String? = null
) : Packet()

data class ChatMessagePacket(
    val senderUuid: String,
    val senderName: String,
    val channelId: String,
    val message: String,
    val server: String
) : Packet()

data class PrivateMessagePacket(
    val senderUuid: String,
    val senderName: String,
    val recipientUuid: String,
    val message: String
) : Packet()

data class TicketPacket(
    val ticketId: String,
    val action: String,
    val actorUuid: String?,
    val actorName: String?,
    val extra: String? = null
) : Packet()

data class StatUpdatePacket(
    val playerUuid: String,
    val statId: String,
    val newValue: Double
) : Packet()
