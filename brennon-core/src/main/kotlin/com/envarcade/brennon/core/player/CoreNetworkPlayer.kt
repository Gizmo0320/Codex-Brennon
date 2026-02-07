package com.envarcade.brennon.core.player

import com.envarcade.brennon.api.player.NetworkPlayer
import com.envarcade.brennon.api.rank.Rank
import com.envarcade.brennon.common.model.PlayerData
import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.rank.CoreRankManager
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.UUID

/**
 * Core implementation of a NetworkPlayer backed by PlayerData.
 *
 * This wraps the persistent PlayerData model and provides the API
 * contract. Online players are cached; offline players are loaded on demand.
 */
class CoreNetworkPlayer(
    private var data: PlayerData,
    private val rankManager: CoreRankManager,
    private val messageSender: (UUID, Component) -> Unit = { _, _ -> }
) : NetworkPlayer {

    @Volatile
    private var online: Boolean = false

    @Volatile
    private var currentServer: String? = null

    override fun getUniqueId(): UUID = data.uuid

    override fun getName(): String = data.name

    override fun getDisplayName(): Component {
        val rank = getRank()
        val prefix = rank.prefix
        val name = Component.text(data.name)
        return Component.empty().append(prefix).append(Component.text(" ")).append(name)
    }

    override fun getRank(): Rank {
        return rankManager.getRank(data.primaryRank).orElse(rankManager.getDefaultRank())
    }

    override fun getRanks(): Set<Rank> {
        return data.ranks.mapNotNull { rankManager.getRank(it).orElse(null) }.toSet()
    }

    override fun hasPermission(permission: String): Boolean {
        // Check direct permissions first
        if (data.permissions.contains(permission)) return true
        if (data.permissions.contains("*")) return true

        // Check negated permissions
        if (data.permissions.contains("-$permission")) return false

        // Check all ranks (primary + secondary) for the permission
        return getRanks().any { it.hasPermission(permission) }
    }

    override fun getCurrentServer(): String? = currentServer

    override fun getFirstJoin(): Instant = data.firstJoin

    override fun getLastSeen(): Instant = data.lastSeen

    override fun isOnline(): Boolean = online

    override fun sendMessage(message: Component) {
        if (online) {
            messageSender(data.uuid, message)
        }
    }

    // ============================================================
    // Internal methods (not part of the API)
    // ============================================================

    /**
     * Updates the backing data model.
     */
    fun updateData(updater: (PlayerData) -> Unit) {
        updater(data)
    }

    /**
     * Gets the raw data model (for persistence).
     */
    fun getData(): PlayerData = data

    /**
     * Sets the online state and server.
     */
    fun setOnline(server: String) {
        online = true
        currentServer = server
        data.lastSeen = Instant.now()
        data.lastServer = server
    }

    /**
     * Sets the player as offline.
     */
    fun setOffline() {
        online = false
        data.lastSeen = Instant.now()
        currentServer = null
    }

    /**
     * Updates the player's name (for name changes).
     */
    fun updateName(name: String) {
        data.name = name
    }

    /**
     * Sets the primary rank directly.
     */
    fun setPrimaryRank(rankId: String) {
        data.primaryRank = rankId
        if (!data.ranks.contains(rankId)) {
            data.ranks.add(rankId)
        }
    }

    /**
     * Adds a secondary rank.
     */
    fun addRank(rankId: String) {
        data.ranks.add(rankId)
    }

    /**
     * Removes a rank.
     */
    fun removeRank(rankId: String) {
        data.ranks.remove(rankId)
        // If removing the primary rank, fall back to default
        if (data.primaryRank == rankId) {
            data.primaryRank = rankManager.getDefaultRank().id
        }
    }
}
