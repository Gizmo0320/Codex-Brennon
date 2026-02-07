package com.envarcade.brennon.webserver.ws

import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import com.google.gson.Gson
import com.google.gson.JsonParser

class RedisEventBridge(
    private val redis: RedisMessagingService,
    private val wsHandler: WebSocketHandler
) {

    private val gson = Gson()

    private val channelMapping = mapOf(
        Channels.PLAYER_JOIN to "player_join",
        Channels.PLAYER_QUIT to "player_quit",
        Channels.PLAYER_SWITCH to "player_switch",
        Channels.CHAT_MESSAGE to "chat_message",
        Channels.CHAT_PRIVATE to "chat_private",
        Channels.PUNISHMENT_ISSUED to "punishment_created",
        Channels.PUNISHMENT_REVOKED to "punishment_revoked",
        Channels.TICKET_CREATE to "ticket_created",
        Channels.TICKET_UPDATE to "ticket_updated",
        Channels.TICKET_REPLY to "ticket_reply",
        Channels.ECONOMY_UPDATE to "economy_transaction",
        Channels.SERVER_STATUS to "server_status",
        Channels.STAFF_ALERT to "staff_alert",
        Channels.STAFF_CHAT to "staff_chat",
        Channels.RANK_UPDATE to "rank_update",
        Channels.STAT_UPDATE to "stat_update",
        Channels.BROADCAST to "broadcast",
        Channels.SERVER_REGISTRY_UPDATE to "server_registry_update"
    )

    fun start() {
        println("[Brennon] Starting Redis → WebSocket event bridge...")

        for ((redisChannel, wsEventType) in channelMapping) {
            redis.subscribe(redisChannel) { _, message ->
                try {
                    val data = try {
                        val json = JsonParser.parseString(message).asJsonObject
                        json.entrySet().associate { it.key to parseJsonValue(it.value) }
                    } catch (_: Exception) {
                        mapOf("raw" to message)
                    }

                    wsHandler.broadcast(wsEventType, data)
                } catch (e: Exception) {
                    println("[Brennon] Error bridging event $wsEventType: ${e.message}")
                }
            }
        }

        println("[Brennon] Event bridge active — ${channelMapping.size} channels mapped.")
    }

    private fun parseJsonValue(element: com.google.gson.JsonElement): Any? {
        return when {
            element.isJsonNull -> null
            element.isJsonPrimitive -> {
                val prim = element.asJsonPrimitive
                when {
                    prim.isBoolean -> prim.asBoolean
                    prim.isNumber -> prim.asNumber
                    else -> prim.asString
                }
            }
            element.isJsonArray -> element.asJsonArray.map { parseJsonValue(it) }
            element.isJsonObject -> element.asJsonObject.entrySet().associate { it.key to parseJsonValue(it.value) }
            else -> element.toString()
        }
    }
}
