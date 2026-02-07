package com.envarcade.brennon.core.player

import com.envarcade.brennon.api.player.NetworkPlayer
import com.envarcade.brennon.api.player.PlayerManager
import com.envarcade.brennon.common.model.PlayerData
import com.envarcade.brennon.common.model.PlayerSession
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.PlayerNetworkJoinEvent
import com.envarcade.brennon.core.event.PlayerNetworkQuitEvent
import com.envarcade.brennon.core.rank.CoreRankManager
import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Core implementation of the PlayerManager.
 *
 * Manages player data loading, caching, online sessions, and persistence.
 * Online players are kept in an in-memory cache and synced to the database
 * on disconnect and periodically.
 */
class CorePlayerManager(
    private val database: DatabaseManager,
    private val messaging: RedisMessagingService,
    private val rankManager: CoreRankManager,
    private val eventBus: CoreEventBus,
    private val serverName: String
) : PlayerManager {

    /** Cache of online players keyed by UUID */
    private val onlinePlayers = ConcurrentHashMap<UUID, CoreNetworkPlayer>()

    /** Cache of online players keyed by lowercase name (for fast lookup) */
    private val nameCache = ConcurrentHashMap<String, UUID>()

    /** Platform-specific message sender, set by the platform plugin */
    var messageSender: (UUID, Component) -> Unit = { _, _ -> }

    // ============================================================
    // API Implementation
    // ============================================================

    override fun getPlayer(uuid: UUID): CompletableFuture<Optional<NetworkPlayer>> {
        // Check online cache first
        val online = onlinePlayers[uuid]
        if (online != null) {
            return CompletableFuture.completedFuture(Optional.of(online))
        }

        // Load from database
        return database.players.findByUuid(uuid).thenApply { data ->
            if (data != null) {
                Optional.of(CoreNetworkPlayer(data, rankManager, messageSender) as NetworkPlayer)
            } else {
                Optional.empty()
            }
        }
    }

    override fun getPlayer(name: String): CompletableFuture<Optional<NetworkPlayer>> {
        // Check online cache by name
        val uuid = nameCache[name.lowercase()]
        if (uuid != null) {
            val online = onlinePlayers[uuid]
            if (online != null) {
                return CompletableFuture.completedFuture(Optional.of(online))
            }
        }

        // Load from database
        return database.players.findByName(name).thenApply { data ->
            if (data != null) {
                Optional.of(CoreNetworkPlayer(data, rankManager, messageSender) as NetworkPlayer)
            } else {
                Optional.empty()
            }
        }
    }

    override fun getOnlinePlayer(uuid: UUID): NetworkPlayer? = onlinePlayers[uuid]

    override fun isOnline(uuid: UUID): Boolean = onlinePlayers.containsKey(uuid)

    override fun getOnlineCount(): Int = onlinePlayers.size

    // ============================================================
    // Internal Lifecycle Methods
    // ============================================================

    /**
     * Handles a player joining the network.
     *
     * Loads or creates their data, caches them, and fires events.
     */
    fun handleJoin(uuid: UUID, name: String, server: String, ip: String): CompletableFuture<CoreNetworkPlayer> {
        return database.players.findByUuid(uuid).thenApply { existing ->
            val data = existing ?: PlayerData(
                uuid = uuid,
                name = name,
                firstJoin = Instant.now(),
                lastSeen = Instant.now(),
                ipAddress = ip,
                lastServer = server
            )

            // Update name if changed
            data.name = name
            data.lastSeen = Instant.now()
            data.ipAddress = ip
            data.lastServer = server

            val player = CoreNetworkPlayer(data, rankManager, messageSender)
            player.setOnline(server)

            // Cache
            onlinePlayers[uuid] = player
            nameCache[name.lowercase()] = uuid

            // Save to database
            database.players.save(data)

            // Store session in Redis
            storeSession(uuid, name, server)

            // Fire event
            eventBus.publish(PlayerNetworkJoinEvent(uuid, name, server))

            player
        }
    }

    /**
     * Handles a player leaving the network.
     *
     * Persists their data, removes from cache, and fires events.
     */
    fun handleQuit(uuid: UUID): CompletableFuture<Void> {
        val player = onlinePlayers.remove(uuid) ?: return CompletableFuture.completedFuture(null)

        nameCache.remove(player.getName().lowercase())
        player.setOffline()

        // Fire event
        eventBus.publish(PlayerNetworkQuitEvent(uuid, player.getName(), player.getCurrentServer() ?: serverName))

        // Persist final state
        val future = database.players.save(player.getData())

        // Remove Redis session
        removeSession(uuid)

        return future
    }

    /**
     * Handles a player switching servers.
     */
    fun handleServerSwitch(uuid: UUID, newServer: String) {
        val player = onlinePlayers[uuid] ?: return
        player.setOnline(newServer)
        storeSession(uuid, player.getName(), newServer)
    }

    /**
     * Gets a cached online player for internal use.
     */
    fun getCachedPlayer(uuid: UUID): CoreNetworkPlayer? = onlinePlayers[uuid]

    /**
     * Gets all online players.
     */
    fun getOnlinePlayers(): Collection<CoreNetworkPlayer> = onlinePlayers.values

    /**
     * Saves all online player data to the database.
     * Called periodically and on shutdown.
     */
    fun saveAll(): CompletableFuture<Void> {
        val futures = onlinePlayers.values.map { player ->
            database.players.save(player.getData())
        }
        return CompletableFuture.allOf(*futures.toTypedArray())
    }

    /**
     * Clears all caches. Called on shutdown.
     */
    fun shutdown() {
        saveAll().join()
        onlinePlayers.clear()
        nameCache.clear()
    }

    // ============================================================
    // Redis Session Management
    // ============================================================

    private fun storeSession(uuid: UUID, name: String, server: String) {
        try {
            messaging.getPool().resource.use { jedis ->
                val key = "brennon:session:$uuid"
                jedis.hset(key, mapOf(
                    "name" to name,
                    "server" to server,
                    "proxy" to serverName,
                    "connectedAt" to Instant.now().toEpochMilli().toString()
                ))
                jedis.expire(key, 300) // 5 min TTL, refreshed on activity
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to store session for $name: ${e.message}")
        }
    }

    private fun removeSession(uuid: UUID) {
        try {
            messaging.getPool().resource.use { jedis ->
                jedis.del("brennon:session:$uuid")
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to remove session for $uuid: ${e.message}")
        }
    }

    // ============================================================
    // Network-Wide Session Queries (Redis)
    // ============================================================

    /**
     * Refreshes the TTL on all locally cached player sessions.
     * Called periodically by the scheduler to prevent session expiry.
     */
    fun refreshAllSessions() {
        try {
            messaging.getPool().resource.use { jedis ->
                for (player in onlinePlayers.values) {
                    val key = "brennon:session:${player.getUniqueId()}"
                    jedis.expire(key, 300)
                }
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to refresh sessions: ${e.message}")
        }
    }

    /**
     * Checks if a player is online ANYWHERE on the network via Redis.
     * Unlike [isOnline] which only checks the local cache, this queries
     * all servers through Redis session keys.
     *
     * @param uuid The player's UUID
     * @return true if the player has an active session on any server
     */
    fun isOnlineNetwork(uuid: UUID): Boolean {
        // Check local cache first (fastest)
        if (onlinePlayers.containsKey(uuid)) return true

        // Check Redis session
        return try {
            messaging.getPool().resource.use { jedis ->
                jedis.exists("brennon:session:$uuid")
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to check network session for $uuid: ${e.message}")
            false
        }
    }

    /**
     * Gets the server a player is connected to across the network.
     *
     * @param uuid The player's UUID
     * @return The server name, or null if offline
     */
    fun getNetworkServer(uuid: UUID): String? {
        // Check local cache first
        val local = onlinePlayers[uuid]
        if (local != null) return local.getCurrentServer()

        // Check Redis
        return try {
            messaging.getPool().resource.use { jedis ->
                jedis.hget("brennon:session:$uuid", "server")
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets the total number of players online across the entire network via Redis.
     */
    fun getNetworkOnlineCount(): Int {
        return try {
            messaging.getPool().resource.use { jedis ->
                val keys = jedis.keys("brennon:session:*")
                keys.size
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to count network sessions: ${e.message}")
            onlinePlayers.size // Fallback to local count
        }
    }

    /**
     * Gets all player sessions from Redis (network-wide).
     *
     * @return A map of UUID -> server name for all online players
     */
    fun getNetworkSessions(): Map<UUID, String> {
        return try {
            messaging.getPool().resource.use { jedis ->
                val keys = jedis.keys("brennon:session:*")
                val sessions = mutableMapOf<UUID, String>()
                for (key in keys) {
                    val uuidStr = key.removePrefix("brennon:session:")
                    val server = jedis.hget(key, "server") ?: continue
                    try {
                        sessions[UUID.fromString(uuidStr)] = server
                    } catch (_: IllegalArgumentException) { }
                }
                sessions
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to fetch network sessions: ${e.message}")
            // Fallback to local
            onlinePlayers.mapValues { it.value.getCurrentServer() ?: "" }
                .filterValues { it.isNotEmpty() }
        }
    }
}
