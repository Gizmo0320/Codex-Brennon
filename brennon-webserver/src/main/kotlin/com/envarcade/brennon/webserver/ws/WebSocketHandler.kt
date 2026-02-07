package com.envarcade.brennon.webserver.ws

import com.envarcade.brennon.webserver.auth.JwtAuth
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import java.util.concurrent.ConcurrentHashMap

class WebSocketHandler(private val jwtAuth: JwtAuth) {

    private val sessions = ConcurrentHashMap<WsContext, ClientSession>()
    private val gson = Gson()

    fun configure(ws: WsConfig) {
        ws.onConnect { ctx ->
            // Authenticate via query param
            val token = ctx.queryParam("token")
            if (token == null || !jwtAuth.validateToken(token)) {
                ctx.closeSession(4001, "Unauthorized")
                return@onConnect
            }

            val username = jwtAuth.getUsername(token) ?: "unknown"
            sessions[ctx] = ClientSession(username, mutableSetOf())
            println("[Brennon] WebSocket connected: $username")
        }

        ws.onMessage { ctx ->
            val session = sessions[ctx] ?: return@onMessage

            try {
                val json = JsonParser.parseString(ctx.message()).asJsonObject
                val type = json.get("type")?.asString ?: return@onMessage

                when (type) {
                    "subscribe" -> {
                        val events = json.getAsJsonObject("data")
                            ?.getAsJsonArray("events")
                            ?.map { it.asString }
                            ?: return@onMessage
                        session.subscriptions.addAll(events)
                        ctx.send(gson.toJson(mapOf(
                            "type" to "subscribed",
                            "data" to mapOf("events" to session.subscriptions)
                        )))
                    }
                    "unsubscribe" -> {
                        val events = json.getAsJsonObject("data")
                            ?.getAsJsonArray("events")
                            ?.map { it.asString }
                            ?: return@onMessage
                        session.subscriptions.removeAll(events.toSet())
                        ctx.send(gson.toJson(mapOf(
                            "type" to "unsubscribed",
                            "data" to mapOf("events" to session.subscriptions)
                        )))
                    }
                    "ping" -> {
                        ctx.send(gson.toJson(mapOf("type" to "pong")))
                    }
                }
            } catch (e: Exception) {
                println("[Brennon] WebSocket message error: ${e.message}")
            }
        }

        ws.onClose { ctx ->
            val session = sessions.remove(ctx)
            if (session != null) {
                println("[Brennon] WebSocket disconnected: ${session.username}")
            }
        }

        ws.onError { ctx ->
            sessions.remove(ctx)
        }
    }

    fun broadcast(eventType: String, data: Map<String, Any?>) {
        val message = gson.toJson(mapOf(
            "type" to eventType,
            "data" to data,
            "timestamp" to System.currentTimeMillis()
        ))

        sessions.forEach { (ctx, session) ->
            try {
                // Send if client has no subscriptions (gets everything) or is subscribed to this event
                if (session.subscriptions.isEmpty() || eventType in session.subscriptions) {
                    ctx.send(message)
                }
            } catch (e: Exception) {
                // Connection likely closed
                sessions.remove(ctx)
            }
        }
    }

    fun getConnectedCount(): Int = sessions.size

    data class ClientSession(
        val username: String,
        val subscriptions: MutableSet<String>
    )
}
