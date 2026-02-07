package com.envarcade.brennon.proxy.server

import com.envarcade.brennon.common.config.BrennonConfig
import com.envarcade.brennon.common.model.ServerDefinition
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.ServerRegisteredEvent
import com.envarcade.brennon.core.event.ServerUnregisteredEvent
import com.envarcade.brennon.core.server.CoreServerManager
import com.envarcade.brennon.core.server.ServerRegistryService
import com.velocitypowered.api.proxy.ProxyServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Bridges the ServerRegistryService to Velocity's runtime server registration API.
 *
 * Handles:
 * - Syncing all registered servers to Velocity on startup
 * - Registering/unregistering servers when the registry changes
 * - Auto-registration from heartbeats (when enabled)
 * - Auto-unregistration of offline servers (when enabled)
 */
class VelocityServerSync(
    private val proxy: ProxyServer,
    private val registryService: ServerRegistryService,
    private val serverManager: CoreServerManager,
    private val eventBus: CoreEventBus,
    private val config: BrennonConfig
) {

    private val autoUnregisterExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Brennon-AutoUnregister").apply { isDaemon = true }
    }

    /** Tracks when auto-registered servers were last seen online */
    private val lastOnlineTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun initialize() {
        // Sync all existing registry entries to Velocity
        syncAllServers()

        // Listen for registry change events
        eventBus.subscribe(ServerRegisteredEvent::class.java) { event ->
            registerWithVelocity(ServerDefinition(
                name = event.serverName,
                group = event.group,
                host = event.host,
                port = event.port,
                autoRegistered = event.autoRegistered
            ))
        }

        eventBus.subscribe(ServerUnregisteredEvent::class.java) { event ->
            unregisterFromVelocity(event.serverName)
        }

        // Schedule auto-unregistration check if enabled
        if (config.serverRegistry.autoUnregister) {
            autoUnregisterExecutor.scheduleAtFixedRate({
                checkAutoUnregister()
            }, 30, 15, TimeUnit.SECONDS)
        }

        println("[Brennon] Velocity server sync initialized.")
    }

    fun shutdown() {
        autoUnregisterExecutor.shutdownNow()
    }

    /**
     * Called when a heartbeat arrives with host/port for a server.
     * If auto-registration is enabled and the server is not in the registry, register it.
     */
    fun onFirstHeartbeat(name: String, group: String, host: String, port: Int) {
        if (!config.serverRegistry.autoRegistration) return
        if (registryService.getServerDefinition(name) != null) {
            // Already registered — just track that it's alive
            lastOnlineTimestamps[name] = System.currentTimeMillis()
            return
        }

        val def = ServerDefinition(
            name = name,
            group = group,
            host = host,
            port = port,
            autoRegistered = true,
            addedBy = "auto"
        )

        if (registryService.registerServer(def)) {
            lastOnlineTimestamps[name] = System.currentTimeMillis()
            println("[Brennon] Auto-registered server: $name ($host:$port) in group '$group'")
        }
    }

    /**
     * Registers a server definition directly with Velocity.
     * Used for on-demand registration when a player transfer targets
     * a server that exists in the registry but isn't yet in Velocity.
     */
    fun onServerRegistered(def: ServerDefinition) {
        registerWithVelocity(def)
    }

    // ============================================================
    // Internal
    // ============================================================

    private fun syncAllServers() {
        var count = 0
        for (def in registryService.getAllServerDefinitions()) {
            registerWithVelocity(def)
            count++
        }
        if (count > 0) {
            println("[Brennon] Synced $count servers from registry to Velocity.")
        }
    }

    private fun registerWithVelocity(def: ServerDefinition) {
        try {
            val existing = proxy.getServer(def.name)
            if (existing.isPresent) return // Already registered

            val serverInfo = com.velocitypowered.api.proxy.server.ServerInfo(
                def.name,
                InetSocketAddress(def.host, def.port)
            )
            proxy.registerServer(serverInfo)
        } catch (e: Exception) {
            println("[Brennon] Failed to register server '${def.name}' with Velocity: ${e.message}")
        }
    }

    private fun unregisterFromVelocity(serverName: String) {
        try {
            val registered = proxy.getServer(serverName).orElse(null) ?: return

            // Evacuate players to fallback group
            val players = registered.playersConnected
            if (players.isNotEmpty()) {
                val fallback = serverManager.getLeastLoadedServer(config.serverRegistry.fallbackGroup)
                if (fallback != null) {
                    val fallbackServer = proxy.getServer(fallback.name).orElse(null)
                    if (fallbackServer != null) {
                        for (player in players) {
                            player.createConnectionRequest(fallbackServer).fireAndForget()
                        }
                        println("[Brennon] Evacuated ${players.size} players from '$serverName' to '${fallback.name}'")
                    }
                }
            }

            proxy.unregisterServer(registered.serverInfo)
            lastOnlineTimestamps.remove(serverName)
        } catch (e: Exception) {
            println("[Brennon] Failed to unregister server '$serverName' from Velocity: ${e.message}")
        }
    }

    private fun checkAutoUnregister() {
        val timeout = config.serverRegistry.unregisterTimeoutMs
        val now = System.currentTimeMillis()

        for (def in registryService.getAllServerDefinitions()) {
            if (!def.autoRegistered) continue // Only auto-unregister auto-registered servers

            val server = serverManager.getServer(def.name).orElse(null)
            val isOnline = server?.isOnline == true

            if (isOnline) {
                lastOnlineTimestamps[def.name] = now
            } else {
                val lastSeen = lastOnlineTimestamps[def.name] ?: now.also { lastOnlineTimestamps[def.name] = it }
                if (now - lastSeen > timeout) {
                    println("[Brennon] Auto-unregistering offline server: ${def.name} (offline for ${(now - lastSeen) / 1000}s)")
                    registryService.unregisterServer(def.name)
                }
            }
        }
    }
}
