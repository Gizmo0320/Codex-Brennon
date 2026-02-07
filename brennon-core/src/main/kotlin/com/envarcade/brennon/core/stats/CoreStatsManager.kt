package com.envarcade.brennon.core.stats

import com.envarcade.brennon.api.stats.StatsManager
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.StatChangeEvent
import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.packet.Packet
import com.envarcade.brennon.messaging.packet.StatUpdatePacket
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class CoreStatsManager(
    private val database: DatabaseManager,
    private val messaging: RedisMessagingService,
    private val eventBus: CoreEventBus
) : StatsManager {

    private val statCache = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Double>>()

    override fun getStat(player: UUID, statId: String): CompletableFuture<Double> {
        val cached = statCache[player]?.get(statId)
        if (cached != null) return CompletableFuture.completedFuture(cached)
        return database.stats.getStat(player, statId)
    }

    override fun getAllStats(player: UUID): CompletableFuture<Map<String, Double>> {
        val cached = statCache[player]
        if (cached != null && cached.isNotEmpty()) {
            return CompletableFuture.completedFuture(HashMap(cached))
        }
        return database.stats.getAllStats(player)
    }

    override fun incrementStat(player: UUID, statId: String, amount: Double): CompletableFuture<Void> {
        val playerCache = statCache.computeIfAbsent(player) { ConcurrentHashMap() }
        val oldValue = playerCache.getOrDefault(statId, 0.0)
        val newValue = oldValue + amount
        playerCache[statId] = newValue

        eventBus.publish(StatChangeEvent(player, statId, oldValue, newValue))

        return database.stats.incrementStat(player, statId, amount).thenRun {
            val packet = StatUpdatePacket(player.toString(), statId, newValue)
            messaging.publish(Channels.STAT_UPDATE, Packet.serialize(packet))
        }
    }

    override fun setStat(player: UUID, statId: String, value: Double): CompletableFuture<Void> {
        val playerCache = statCache.computeIfAbsent(player) { ConcurrentHashMap() }
        val oldValue = playerCache.getOrDefault(statId, 0.0)
        playerCache[statId] = value

        eventBus.publish(StatChangeEvent(player, statId, oldValue, value))

        return database.stats.setStat(player, statId, value)
    }

    override fun getLeaderboard(statId: String, limit: Int): CompletableFuture<Map<UUID, Double>> {
        return database.stats.getLeaderboard(statId, limit)
    }

    override fun getLeaderboardPosition(player: UUID, statId: String): CompletableFuture<Int> {
        return database.stats.getLeaderboardPosition(player, statId).thenApply { it as Int }
    }

    override fun resetStat(player: UUID, statId: String): CompletableFuture<Void> {
        statCache[player]?.remove(statId)
        return database.stats.resetStat(player, statId)
    }

    override fun resetAllStats(player: UUID): CompletableFuture<Void> {
        statCache.remove(player)
        return database.stats.resetAllStats(player)
    }

    /**
     * Pre-loads a player's stats into cache when they join.
     */
    fun handlePlayerJoin(uuid: UUID) {
        database.stats.getAllStats(uuid).thenAccept { stats ->
            val cache = ConcurrentHashMap<String, Double>()
            cache.putAll(stats)
            statCache[uuid] = cache
        }
    }

    /**
     * Flushes a player's cached stats to DB when they quit.
     */
    fun handlePlayerQuit(uuid: UUID) {
        val cached = statCache.remove(uuid) ?: return
        for ((statId, value) in cached) {
            database.stats.setStat(uuid, statId, value)
        }
    }

    /**
     * Flushes all cached stats to DB. Called by the scheduler.
     */
    fun flushAll() {
        for ((uuid, stats) in statCache) {
            for ((statId, value) in stats) {
                database.stats.setStat(uuid, statId, value)
            }
        }
    }
}
