package com.envarcade.brennon.core.staff

import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages staff mode (vanish, inspection mode) across the network.
 *
 * Staff mode state is tracked locally and synced via Redis.
 * All Redis operations are async to avoid blocking the server main thread.
 */
class StaffManager(
    private val messaging: RedisMessagingService,
    private val serverName: String
) {

    /** Players currently in staff mode */
    private val staffMode = ConcurrentHashMap<UUID, StaffState>()

    /** Players currently vanished */
    private val vanished = ConcurrentHashMap.newKeySet<UUID>()

    data class StaffState(
        val uuid: UUID,
        val name: String,
        val server: String,
        val enabledAt: Long = System.currentTimeMillis(),
        var vanished: Boolean = false
    )

    /**
     * Toggles staff mode for a player.
     *
     * @return CompletableFuture resolving to true if staff mode is now enabled
     */
    fun toggleStaffMode(uuid: UUID, name: String): CompletableFuture<Boolean> {
        return if (staffMode.containsKey(uuid)) {
            disableStaffMode(uuid).thenApply { false }
        } else {
            enableStaffMode(uuid, name).thenApply { true }
        }
    }

    /**
     * Enables staff mode for a player.
     */
    fun enableStaffMode(uuid: UUID, name: String): CompletableFuture<Void> {
        val state = StaffState(uuid, name, serverName)
        staffMode[uuid] = state

        return CompletableFuture.runAsync {
            // Store in Redis so other servers know
            try {
                messaging.getPool().resource.use { jedis ->
                    jedis.hset("brennon:staff:$uuid", mapOf(
                        "name" to name,
                        "server" to serverName,
                        "vanished" to "false"
                    ))
                }
            } catch (e: Exception) {
                println("[Brennon] Failed to sync staff mode for $name: ${e.message}")
            }

            messaging.publish(Channels.STAFF_ALERT, """
                {"type":"staff_mode","uuid":"$uuid","name":"$name","enabled":true,"server":"$serverName"}
            """.trimIndent())
        }
    }

    /**
     * Disables staff mode for a player.
     */
    fun disableStaffMode(uuid: UUID): CompletableFuture<Void> {
        val state = staffMode.remove(uuid) ?: return CompletableFuture.completedFuture(null)
        vanished.remove(uuid)

        return CompletableFuture.runAsync {
            try {
                messaging.getPool().resource.use { jedis ->
                    jedis.del("brennon:staff:$uuid")
                }
            } catch (_: Exception) { }

            messaging.publish(Channels.STAFF_ALERT, """
                {"type":"staff_mode","uuid":"$uuid","name":"${state.name}","enabled":false,"server":"$serverName"}
            """.trimIndent())
        }
    }

    /**
     * Toggles vanish for a player.
     *
     * @return CompletableFuture resolving to true if the player is now vanished
     */
    fun toggleVanish(uuid: UUID): CompletableFuture<Boolean> {
        return if (vanished.contains(uuid)) {
            vanished.remove(uuid)
            staffMode[uuid]?.vanished = false
            updateVanishState(uuid, false).thenApply { false }
        } else {
            vanished.add(uuid)
            staffMode[uuid]?.vanished = true
            updateVanishState(uuid, true).thenApply { true }
        }
    }

    /**
     * Checks if a player is in staff mode.
     */
    fun isInStaffMode(uuid: UUID): Boolean = staffMode.containsKey(uuid)

    /**
     * Checks if a player is vanished.
     */
    fun isVanished(uuid: UUID): Boolean = vanished.contains(uuid)

    /**
     * Gets all players currently in staff mode on this server.
     */
    fun getLocalStaff(): Collection<StaffState> = staffMode.values

    /**
     * Gets all vanished player UUIDs.
     */
    fun getVanishedPlayers(): Set<UUID> = vanished.toSet()

    /**
     * Gets all staff members across the network from Redis.
     */
    fun getNetworkStaff(): CompletableFuture<Map<UUID, Map<String, String>>> {
        return CompletableFuture.supplyAsync {
            try {
                messaging.getPool().resource.use { jedis ->
                    val keys = jedis.keys("brennon:staff:*")
                    val result = mutableMapOf<UUID, Map<String, String>>()
                    for (key in keys) {
                        val uuidStr = key.removePrefix("brennon:staff:")
                        try {
                            val data = jedis.hgetAll(key)
                            result[UUID.fromString(uuidStr)] = data
                        } catch (_: Exception) { }
                    }
                    result
                }
            } catch (e: Exception) {
                println("[Brennon] Failed to fetch network staff: ${e.message}")
                emptyMap()
            }
        }
    }

    /**
     * Cleans up when a staff member disconnects.
     */
    fun handleDisconnect(uuid: UUID) {
        disableStaffMode(uuid)
    }

    private fun updateVanishState(uuid: UUID, vanished: Boolean): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            try {
                messaging.getPool().resource.use { jedis ->
                    jedis.hset("brennon:staff:$uuid", "vanished", vanished.toString())
                }
            } catch (_: Exception) { }
        }
    }
}
