package com.envarcade.brennon.core.server

import com.envarcade.brennon.api.server.ServerGroupInfo
import com.envarcade.brennon.api.server.ServerInfo
import com.envarcade.brennon.api.server.ServerManager
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Core implementation of the ServerManager.
 *
 * Tracks server status via Redis heartbeats and provides
 * routing capabilities for sending players between servers.
 */
class CoreServerManager(
    private val messaging: RedisMessagingService,
    private val currentServerName: String,
    private val currentServerGroup: String,
    private val networkId: String = "main"
) : ServerManager {

    private val servers = ConcurrentHashMap<String, CoreServerInfo>()
    private val gson = Gson()
    private val heartbeatExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Brennon-Heartbeat").apply { isDaemon = true }
    }
    private var heartbeatActive = false

    /** Platform-specific player sender (set by Velocity/Bukkit) */
    var playerSender: (UUID, String) -> CompletableFuture<Void> = { _, _ ->
        CompletableFuture.failedFuture(UnsupportedOperationException("Player sending not available on this platform."))
    }

    /** Override this with platform-specific player count */
    var localPlayerCountProvider: () -> Int = { 0 }

    /** Platform-provided host for auto-registration heartbeats */
    var localHostProvider: (() -> String)? = null

    /** Platform-provided port for auto-registration heartbeats */
    var localPortProvider: (() -> Int)? = null

    /** Called by proxy when a heartbeat arrives with host/port for a new server */
    var autoRegistrationCallback: ((name: String, group: String, host: String, port: Int) -> Unit)? = null

    /** Server registry service — set by Brennon bootstrap */
    var registryService: ServerRegistryService? = null

    /**
     * Starts the heartbeat system and subscribes to server status updates.
     * @param sendHeartbeats If false, only subscribes to status updates without broadcasting heartbeats.
     */
    fun initialize(sendHeartbeats: Boolean = true) {
        // Subscribe to server status channel
        messaging.subscribe(Channels.SERVER_STATUS) { _, message ->
            try {
                val json = gson.fromJson(message, JsonObject::class.java)
                val name = json.get("name").asString
                val group = json.get("group").asString
                val players = json.get("players").asInt
                val maxPlayers = json.get("maxPlayers")?.asInt ?: 100
                val online = json.get("online")?.asBoolean ?: true
                val motd = json.get("motd")?.asString ?: ""

                val server = servers.computeIfAbsent(name) {
                    CoreServerInfo(name, group, players, maxPlayers, online, motd)
                }
                server.updateHeartbeat(players, online)

                // Auto-registration: if heartbeat includes host/port, notify callback
                val host = json.get("host")?.asString
                val port = json.get("port")?.asInt
                if (host != null && host.isNotBlank() && port != null && port > 0) {
                    autoRegistrationCallback?.invoke(name, group, host, port)
                }
            } catch (e: Exception) {
                println("[Brennon] Failed to parse server status: ${e.message}")
            }
        }

        if (sendHeartbeats) {
            // Start sending heartbeats every 10 seconds
            heartbeatExecutor.scheduleAtFixedRate({
                sendHeartbeat()
            }, 0, 10, TimeUnit.SECONDS)
            heartbeatActive = true
        }

        val mode = if (sendHeartbeats) "" else " (read-only, no heartbeat)"
        println("[Brennon] Server manager initialized$mode. Current server: $currentServerName ($currentServerGroup)")
    }

    /**
     * Shuts down the heartbeat system.
     */
    fun shutdown() {
        if (heartbeatActive) {
            heartbeatExecutor.shutdownNow()
            // Send offline heartbeat
            try {
                val json = gson.toJson(mapOf(
                    "name" to currentServerName,
                    "group" to currentServerGroup,
                    "players" to 0,
                    "online" to false,
                    "networkId" to networkId
                ))
                messaging.publish(Channels.SERVER_STATUS, json)
            } catch (_: Exception) { }
        }
    }

    // ============================================================
    // API Implementation
    // ============================================================

    override fun getServer(name: String): Optional<ServerInfo> =
        Optional.ofNullable(servers[name])

    override fun getServers(): Collection<ServerInfo> =
        servers.values.toList()

    override fun getOnlineServers(): Collection<ServerInfo> =
        servers.values.filter { it.isOnline }

    override fun sendPlayer(uuid: UUID, serverName: String): CompletableFuture<Void> {
        val server = servers[serverName]
        if (server == null || !server.isOnline) {
            return CompletableFuture.failedFuture(
                IllegalStateException("Server '$serverName' is not available.")
            )
        }

        return playerSender(uuid, serverName)
    }

    override fun getCurrentServer(): String = currentServerName

    override fun getGroup(groupId: String): Optional<ServerGroupInfo> {
        val registry = registryService ?: return Optional.empty()
        val group = registry.getGroupDefinition(groupId) ?: return Optional.empty()
        return Optional.of(CoreServerGroupInfo(group))
    }

    override fun getGroups(): Collection<ServerGroupInfo> {
        val registry = registryService ?: return emptyList()
        return registry.getAllGroupDefinitions().map { CoreServerGroupInfo(it) }
    }

    override fun getServersByGroup(groupId: String): Collection<ServerInfo> =
        servers.values.filter { it.getGroup() == groupId && it.isOnline }

    // ============================================================
    // Internal
    // ============================================================

    /**
     * Sends a heartbeat with current server status.
     */
    private fun sendHeartbeat() {
        try {
            val heartbeat = mutableMapOf<String, Any>(
                "name" to currentServerName,
                "group" to currentServerGroup,
                "players" to getLocalPlayerCount(),
                "maxPlayers" to 100,
                "online" to true,
                "motd" to "",
                "networkId" to networkId
            )

            val host = localHostProvider?.invoke()
            val port = localPortProvider?.invoke()
            if (host != null && host.isNotBlank()) heartbeat["host"] = host
            if (port != null && port > 0) heartbeat["port"] = port

            messaging.publish(Channels.SERVER_STATUS, gson.toJson(heartbeat))
        } catch (e: Exception) {
            println("[Brennon] Failed to send heartbeat: ${e.message}")
        }
    }

    private fun getLocalPlayerCount(): Int = localPlayerCountProvider()

    /**
     * Gets the least loaded server in a group (for load balancing).
     */
    fun getLeastLoadedServer(group: String): CoreServerInfo? =
        servers.values.filter { it.getGroup() == group && it.isOnline }.minByOrNull { it.playerCount }

    /**
     * Gets the total player count across all known servers.
     */
    fun getNetworkPlayerCount(): Int =
        servers.values.filter { it.isOnline }.sumOf { it.playerCount }
}
