package com.envarcade.brennon.database.repository.mongo

import com.envarcade.brennon.common.config.DataSharingMode
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.database.repository.StatsRepository
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import org.bson.Document
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * MongoDB implementation of the StatsRepository.
 * Network-aware: uses composite _id of `{uuid}:{networkId}` to
 * scope stats per network, or `{uuid}:__global__` for GLOBAL mode.
 */
class MongoStatsRepository(
    database: MongoDatabase,
    private val networkContext: NetworkContext
) : StatsRepository {

    private val collection = database.getCollection("stats")

    private val effectiveNetworkId: String =
        if (networkContext.sharing.stats == DataSharingMode.GLOBAL) "__global__"
        else networkContext.networkId

    private fun docId(uuid: UUID): String = "${uuid}:${effectiveNetworkId}"

    override fun getStat(uuid: UUID, statId: String): CompletableFuture<Double> {
        return CompletableFuture.supplyAsync {
            val doc = collection.find(Filters.eq("_id", docId(uuid))).first()
            doc?.getDouble(statId) ?: 0.0
        }
    }

    override fun getAllStats(uuid: UUID): CompletableFuture<Map<String, Double>> {
        return CompletableFuture.supplyAsync {
            val doc = collection.find(Filters.eq("_id", docId(uuid))).first()
            if (doc == null) return@supplyAsync emptyMap<String, Double>()
            val stats = mutableMapOf<String, Double>()
            for (key in doc.keys) {
                if (key == "_id" || key == "networkId") continue
                val value = doc.get(key)
                if (value is Number) {
                    stats[key] = value.toDouble()
                }
            }
            stats
        }
    }

    override fun setStat(uuid: UUID, statId: String, value: Double): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            collection.updateOne(
                Filters.eq("_id", docId(uuid)),
                Updates.combine(
                    Updates.set(statId, value),
                    Updates.set("networkId", effectiveNetworkId)
                ),
                com.mongodb.client.model.UpdateOptions().upsert(true)
            )
        }
    }

    override fun incrementStat(uuid: UUID, statId: String, amount: Double): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            collection.updateOne(
                Filters.eq("_id", docId(uuid)),
                Updates.combine(
                    Updates.inc(statId, amount),
                    Updates.set("networkId", effectiveNetworkId)
                ),
                com.mongodb.client.model.UpdateOptions().upsert(true)
            )
        }
    }

    override fun getLeaderboard(statId: String, limit: Int): CompletableFuture<Map<UUID, Double>> {
        return CompletableFuture.supplyAsync {
            val results = linkedMapOf<UUID, Double>()
            collection.find(
                Filters.and(
                    Filters.exists(statId),
                    Filters.eq("networkId", effectiveNetworkId)
                )
            )
                .sort(Sorts.descending(statId))
                .limit(limit)
                .forEach { doc ->
                    val compositeId = doc.getString("_id")
                    val uuidStr = compositeId.substringBefore(":")
                    val uuid = UUID.fromString(uuidStr)
                    val value = doc.get(statId)
                    if (value is Number) {
                        results[uuid] = value.toDouble()
                    }
                }
            results
        }
    }

    override fun getLeaderboardPosition(uuid: UUID, statId: String): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            val playerDoc = collection.find(Filters.eq("_id", docId(uuid))).first()
            val playerValue = playerDoc?.get(statId) as? Number ?: return@supplyAsync -1
            val count = collection.countDocuments(
                Filters.and(
                    Filters.gt(statId, playerValue.toDouble()),
                    Filters.eq("networkId", effectiveNetworkId)
                )
            )
            (count + 1).toInt()
        }
    }

    override fun resetStat(uuid: UUID, statId: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            collection.updateOne(
                Filters.eq("_id", docId(uuid)),
                Updates.unset(statId)
            )
        }
    }

    override fun resetAllStats(uuid: UUID): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            collection.deleteOne(Filters.eq("_id", docId(uuid)))
        }
    }
}
