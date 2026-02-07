package com.envarcade.brennon.core.rank

import com.envarcade.brennon.core.player.CorePlayerManager
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import com.google.gson.JsonParser
import java.util.UUID

/**
 * Subscribes to the Redis RANK_UPDATE channel and applies cross-server
 * rank changes to locally cached players.
 *
 * Previously, rank changes published via Redis were never consumed —
 * this class fixes that gap.
 */
class RankUpdateSubscriber(
    private val messaging: RedisMessagingService,
    private val playerManager: CorePlayerManager,
    private val rankManager: CoreRankManager,
    private val luckPermsHook: LuckPermsHook?,
    private val permissionRefreshCallback: (() -> ((UUID) -> Unit)?)
) {

    fun initialize() {
        messaging.subscribe(Channels.RANK_UPDATE) { _, message ->
            try {
                handleRankUpdate(message)
            } catch (e: Exception) {
                println("[Brennon] Error handling rank update: ${e.message}")
            }
        }
        println("[Brennon] Subscribed to rank update channel.")
    }

    private fun handleRankUpdate(message: String) {
        val json = JsonParser.parseString(message).asJsonObject
        val uuid = UUID.fromString(json.get("uuid").asString)
        val action = json.get("action").asString

        // Update locally cached player
        val cachedPlayer = playerManager.getCachedPlayer(uuid) ?: return

        when (action) {
            "set" -> {
                val newRank = json.get("newRank").asString
                cachedPlayer.setPrimaryRank(newRank)
            }
            "add" -> {
                val rank = json.get("rank").asString
                cachedPlayer.addRank(rank)
            }
            "remove" -> {
                val rank = json.get("rank").asString
                cachedPlayer.removeRank(rank)
            }
        }

        // Refresh permissions
        if (luckPermsHook?.isActive == true) {
            // LP handles permission recalculation via its own sync
            return
        }

        // Fallback: use platform permission refresh callback
        permissionRefreshCallback()?.invoke(uuid)
    }
}
