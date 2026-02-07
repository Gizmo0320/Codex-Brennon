package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class ChatRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/chat/channels") { ctx ->
            val channels = brennon.coreChatManager.getChannels().map { ch ->
                mapOf(
                    "id" to ch.id,
                    "displayName" to ch.displayName,
                    "isCrossServer" to ch.isCrossServer,
                    "isDefault" to (ch.id == brennon.coreChatManager.getDefaultChannel().id)
                )
            }
            ctx.json(channels)
        }

        app.get("/api/chat/channels/{id}") { ctx ->
            val id = ctx.pathParam("id")
            val channel = brennon.coreChatManager.getChannel(id)
            if (channel.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Channel not found"))
                return@get
            }
            val ch = channel.get()
            ctx.json(mapOf(
                "id" to ch.id,
                "displayName" to ch.displayName,
                "isCrossServer" to ch.isCrossServer
            ))
        }

        app.get("/api/chat/{uuid}/channel") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val channel = brennon.coreChatManager.getPlayerChannel(uuid)
            ctx.json(mapOf(
                "uuid" to uuid.toString(),
                "channel" to channel.id,
                "displayName" to channel.displayName
            ))
        }

        app.put("/api/chat/{uuid}/channel") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val body = ctx.bodyAsClass(ChannelRequest::class.java)
            brennon.coreChatManager.setPlayerChannel(uuid, body.channelId)
            ctx.json(mapOf("success" to true, "channel" to body.channelId))
        }

        app.post("/api/chat/send") { ctx ->
            val body = ctx.bodyAsClass(ChatSendRequest::class.java)
            val sender = UUID.fromString(body.sender)
            brennon.coreChatManager.sendMessage(sender, body.senderName, body.channelId, body.message)
            ctx.json(mapOf("success" to true))
        }

        app.post("/api/chat/dm") { ctx ->
            val body = ctx.bodyAsClass(DmRequest::class.java)
            val sender = UUID.fromString(body.sender)
            val recipient = UUID.fromString(body.recipient)
            brennon.coreChatManager.sendPrivateMessage(sender, body.senderName, recipient, body.message)
            ctx.json(mapOf("success" to true))
        }
    }

    data class ChannelRequest(val channelId: String = "")
    data class ChatSendRequest(
        val sender: String = "", val senderName: String = "",
        val channelId: String = "global", val message: String = ""
    )
    data class DmRequest(
        val sender: String = "", val senderName: String = "",
        val recipient: String = "", val message: String = ""
    )
}
