package com.envarcade.brennon.core.rank

import com.envarcade.brennon.api.rank.Rank
import com.envarcade.brennon.api.rank.RankManager
import com.envarcade.brennon.common.model.RankData
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.PlayerRankChangeEvent
import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Core implementation of the RankManager.
 *
 * Manages rank loading, caching, inheritance resolution, and
 * cross-server rank synchronization via Redis.
 */
class CoreRankManager(
    private val database: DatabaseManager,
    private val messaging: RedisMessagingService,
    private val eventBus: CoreEventBus
) : RankManager {

    /** Cached ranks keyed by ID */
    private val ranks = ConcurrentHashMap<String, CoreRank>()

    /** The default rank for new players */
    private var defaultRank: CoreRank? = null

    /** LuckPerms integration hook — null if LP not present */
    var luckPermsHook: LuckPermsHook? = null

    /** Set to true during LP->Brennon sync to prevent re-pushing back to LP */
    @Volatile
    var suppressLuckPermsSync: Boolean = false

    // ============================================================
    // Initialization
    // ============================================================

    /**
     * Loads all ranks from the database and resolves inheritance.
     */
    fun initialize(): CompletableFuture<Void> {
        return database.ranks.findAll().thenAccept { rankDataList ->
            ranks.clear()

            for (data in rankDataList) {
                val rank = CoreRank(data)
                ranks[data.id] = rank

                if (data.isDefault) {
                    defaultRank = rank
                }
            }

            // Ensure there's always a default rank
            if (defaultRank == null) {
                val fallback = RankData(
                    id = "default",
                    displayName = "Default",
                    prefix = "<gray>[Default]",
                    weight = 0,
                    isDefault = true
                )
                val rank = CoreRank(fallback)
                ranks["default"] = rank
                defaultRank = rank
                database.ranks.save(fallback)
                println("[Brennon] Created fallback 'default' rank.")
            }

            // Resolve inheritance chains
            resolveInheritance()

            println("[Brennon] Loaded ${ranks.size} ranks.")
        }
    }

    // ============================================================
    // API Implementation
    // ============================================================

    override fun getRank(id: String): Optional<Rank> =
        Optional.ofNullable(ranks[id])

    override fun getRanks(): Collection<Rank> =
        ranks.values.toList()

    override fun getDefaultRank(): Rank =
        defaultRank ?: throw IllegalStateException("No default rank configured!")

    override fun setPlayerRank(uuid: UUID, rankId: String): CompletableFuture<Void> {
        val rank = ranks[rankId]
            ?: return CompletableFuture.failedFuture(
                IllegalArgumentException("Rank '$rankId' does not exist.")
            )

        return database.players.findByUuid(uuid).thenCompose { data ->
            if (data == null) {
                return@thenCompose CompletableFuture.failedFuture<Void>(
                    IllegalArgumentException("Player $uuid not found.")
                )
            }

            val oldRank = data.primaryRank
            data.primaryRank = rankId
            if (!data.ranks.contains(rankId)) {
                data.ranks.add(rankId)
            }

            database.players.save(data).thenRun {
                // Fire event
                eventBus.publish(PlayerRankChangeEvent(uuid, oldRank, rankId, null))

                // Broadcast to other servers
                messaging.publish(Channels.RANK_UPDATE, """
                    {"uuid":"$uuid","oldRank":"$oldRank","newRank":"$rankId","action":"set"}
                """.trimIndent())

                // Sync to LuckPerms
                if (!suppressLuckPermsSync) {
                    luckPermsHook?.pushPlayerRankToLuckPerms(uuid, rankId)
                }
            }
        }
    }

    override fun addPlayerRank(uuid: UUID, rankId: String): CompletableFuture<Void> {
        if (!ranks.containsKey(rankId)) {
            return CompletableFuture.failedFuture(
                IllegalArgumentException("Rank '$rankId' does not exist.")
            )
        }

        return database.players.findByUuid(uuid).thenCompose { data ->
            if (data == null) {
                return@thenCompose CompletableFuture.failedFuture<Void>(
                    IllegalArgumentException("Player $uuid not found.")
                )
            }

            data.ranks.add(rankId)
            database.players.save(data).thenRun {
                messaging.publish(Channels.RANK_UPDATE, """
                    {"uuid":"$uuid","rank":"$rankId","action":"add"}
                """.trimIndent())

                // Sync to LuckPerms
                if (!suppressLuckPermsSync) {
                    luckPermsHook?.addPlayerRankToLuckPerms(uuid, rankId)
                }
            }
        }
    }

    override fun removePlayerRank(uuid: UUID, rankId: String): CompletableFuture<Void> {
        return database.players.findByUuid(uuid).thenCompose { data ->
            if (data == null) {
                return@thenCompose CompletableFuture.failedFuture<Void>(
                    IllegalArgumentException("Player $uuid not found.")
                )
            }

            data.ranks.remove(rankId)
            if (data.primaryRank == rankId) {
                data.primaryRank = defaultRank?.getData()?.id ?: "default"
            }

            database.players.save(data).thenRun {
                messaging.publish(Channels.RANK_UPDATE, """
                    {"uuid":"$uuid","rank":"$rankId","action":"remove"}
                """.trimIndent())

                // Sync to LuckPerms
                if (!suppressLuckPermsSync) {
                    luckPermsHook?.removePlayerRankFromLuckPerms(uuid, rankId)
                }
            }
        }
    }

    // ============================================================
    // Rank CRUD (internal, not part of API)
    // ============================================================

    /**
     * Creates or updates a rank.
     */
    fun saveRank(data: RankData): CompletableFuture<Void> {
        val rank = CoreRank(data)
        ranks[data.id] = rank

        if (data.isDefault) {
            defaultRank = rank
        }

        resolveInheritance()

        // Sync to LuckPerms
        if (!suppressLuckPermsSync) {
            luckPermsHook?.pushRankToLuckPerms(rank)
        }

        return database.ranks.save(data)
    }

    /**
     * Deletes a rank. Players with this rank will be set to default.
     */
    fun deleteRank(id: String): CompletableFuture<Void> {
        if (id == (defaultRank?.getData()?.id ?: "default")) {
            return CompletableFuture.failedFuture(
                IllegalStateException("Cannot delete the default rank.")
            )
        }

        ranks.remove(id)
        resolveInheritance()

        // Sync to LuckPerms
        if (!suppressLuckPermsSync) {
            luckPermsHook?.deleteGroupFromLuckPerms(id)
        }

        return database.ranks.delete(id)
    }

    /**
     * Gets a CoreRank for internal use (casts from Rank).
     */
    fun getCoreRank(id: String): CoreRank? = ranks[id]

    /**
     * Reloads all ranks from the database.
     */
    fun reload(): CompletableFuture<Void> = initialize()

    // ============================================================
    // Inheritance Resolution
    // ============================================================

    /**
     * Resolves inherited permissions for all ranks.
     *
     * Walks the inheritance tree and combines all permissions,
     * handling circular references gracefully.
     */
    private fun resolveInheritance() {
        for (rank in ranks.values) {
            val effective = mutableSetOf<String>()
            resolvePermissions(rank.getData().id, effective, mutableSetOf())
            rank.setEffectivePermissions(effective)
        }
    }

    private fun resolvePermissions(rankId: String, accumulator: MutableSet<String>, visited: MutableSet<String>) {
        if (visited.contains(rankId)) return // Circular reference protection
        visited.add(rankId)

        val rank = ranks[rankId] ?: return

        // Add direct permissions
        accumulator.addAll(rank.getData().permissions)

        // Resolve parent permissions
        for (parentId in rank.getData().inheritance) {
            resolvePermissions(parentId, accumulator, visited)
        }
    }
}
