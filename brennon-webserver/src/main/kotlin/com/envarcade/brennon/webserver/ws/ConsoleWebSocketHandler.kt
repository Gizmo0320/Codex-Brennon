package com.envarcade.brennon.webserver.ws

import com.envarcade.brennon.webserver.PterodactylConfig
import com.envarcade.brennon.webserver.auth.JwtAuth
import com.envarcade.brennon.webserver.pterodactyl.PterodactylClient
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket handler that proxies browser connections to Pterodactyl console WebSockets.
 * Admin-only — validates JWT via query param.
 */
class ConsoleWebSocketHandler(
    private val jwtAuth: JwtAuth,
    private val pteroClient: PterodactylClient,
    private val pteroConfig: PterodactylConfig
) {

    private val sessions = ConcurrentHashMap<WsContext, UpstreamSession>()
    private val httpClient = HttpClient.newBuilder().build()

    fun configure(ws: WsConfig) {
        ws.onConnect { ctx ->
            // Authenticate — admin only
            val token = ctx.queryParam("token")
            if (token == null || !jwtAuth.validateToken(token)) {
                ctx.closeSession(4001, "Unauthorized")
                return@onConnect
            }

            val role = jwtAuth.getRole(token)
            if (role != "admin") {
                ctx.closeSession(4003, "Admin access required")
                return@onConnect
            }

            // Resolve server name to Pterodactyl ID
            val serverName = ctx.pathParam("serverName")
            val pteroId = pteroClient.resolveServerId(serverName)
            if (pteroId == null) {
                ctx.closeSession(4004, "Server not mapped: $serverName")
                return@onConnect
            }

            // Get console WebSocket credentials from Pterodactyl
            try {
                val wsInfo = pteroClient.getConsoleWebSocket(pteroId)
                connectUpstream(ctx, wsInfo, serverName)
            } catch (e: Exception) {
                ctx.closeSession(4005, "Failed to get console WS: ${e.message}")
            }
        }

        ws.onMessage { ctx ->
            // Forward browser messages to Pterodactyl WS
            val session = sessions[ctx] ?: return@onMessage
            session.upstream?.sendText(ctx.message(), true)
        }

        ws.onClose { ctx ->
            val session = sessions.remove(ctx) ?: return@onClose
            session.upstream?.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnected")
            println("[Brennon] Console WS disconnected: ${session.serverName}")
        }

        ws.onError { ctx ->
            val session = sessions.remove(ctx)
            session?.upstream?.sendClose(WebSocket.NORMAL_CLOSURE, "Error")
        }
    }

    private fun connectUpstream(clientCtx: WsContext, wsInfo: PterodactylClient.ConsoleWsInfo, serverName: String) {
        val listener = object : WebSocket.Listener {
            private val buffer = StringBuilder()

            override fun onOpen(webSocket: WebSocket) {
                webSocket.request(1)
                // Send auth event to Pterodactyl WS
                val authPayload = """{"event":"auth","args":["${wsInfo.token}"]}"""
                webSocket.sendText(authPayload, true)
            }

            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                buffer.append(data)
                if (last) {
                    val message = buffer.toString()
                    buffer.setLength(0)
                    try {
                        clientCtx.send(message)
                    } catch (_: Exception) {
                        // Client disconnected
                    }
                }
                webSocket.request(1)
                return null
            }

            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                sessions.remove(clientCtx)
                try {
                    clientCtx.closeSession(statusCode, reason)
                } catch (_: Exception) { }
                return null
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                sessions.remove(clientCtx)
                try {
                    clientCtx.closeSession(4006, "Upstream error: ${error.message}")
                } catch (_: Exception) { }
            }
        }

        val upstream = httpClient.newWebSocketBuilder()
            .header("Origin", wsInfo.socket.substringBefore("/api"))
            .buildAsync(URI.create(wsInfo.socket), listener)
            .join()

        sessions[clientCtx] = UpstreamSession(upstream, serverName)
        println("[Brennon] Console WS connected: $serverName")
    }

    fun getActiveCount(): Int = sessions.size

    private data class UpstreamSession(
        val upstream: WebSocket?,
        val serverName: String
    )
}
