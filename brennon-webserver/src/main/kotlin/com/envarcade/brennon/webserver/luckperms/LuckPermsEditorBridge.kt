package com.envarcade.brennon.webserver.luckperms

import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Bridges the webserver to game servers for LuckPerms editor session creation.
 *
 * Flow:
 * 1. Web dashboard calls requestEditorUrl()
 * 2. Bridge publishes a request to LUCKPERMS_EDITOR_REQUEST Redis channel
 * 3. One game server claims the request (via SETNX lock), runs `lp editor`, captures the URL
 * 4. Game server publishes the URL to LUCKPERMS_EDITOR_RESPONSE
 * 5. Bridge completes the pending future with the URL
 */
class LuckPermsEditorBridge(
    private val redisMessaging: RedisMessagingService
) {

    private val pendingRequests = ConcurrentHashMap<String, CompletableFuture<String>>()

    fun initialize() {
        redisMessaging.subscribe(Channels.LUCKPERMS_EDITOR_RESPONSE) { _, message ->
            try {
                val json = JsonParser.parseString(message).asJsonObject
                val requestId = json.get("requestId").asString
                val future = pendingRequests.remove(requestId) ?: return@subscribe

                if (json.has("error")) {
                    future.completeExceptionally(RuntimeException(json.get("error").asString))
                } else {
                    future.complete(json.get("url").asString)
                }
            } catch (e: Exception) {
                println("[Brennon] Failed to process LP editor response: ${e.message}")
            }
        }
    }

    /**
     * Requests an LP editor session URL from a game server.
     * Returns a future that completes with the editor URL or fails after 15 seconds.
     */
    fun requestEditorUrl(): CompletableFuture<String> {
        val requestId = UUID.randomUUID().toString()
        val future = CompletableFuture<String>()

        pendingRequests[requestId] = future

        // Publish request to game servers
        val request = JsonObject().apply {
            addProperty("requestId", requestId)
        }
        redisMessaging.publish(Channels.LUCKPERMS_EDITOR_REQUEST, request.toString())

        // Timeout after 15 seconds
        return future.orTimeout(15, TimeUnit.SECONDS).exceptionally { e ->
            pendingRequests.remove(requestId)
            if (e is TimeoutException || e.cause is TimeoutException) {
                throw RuntimeException("No game server responded. Ensure at least one server with LuckPerms is online.")
            }
            throw if (e is RuntimeException) e else RuntimeException(e.message ?: "Unknown error", e)
        }
    }

    fun shutdown() {
        pendingRequests.values.forEach { it.cancel(true) }
        pendingRequests.clear()
    }
}
