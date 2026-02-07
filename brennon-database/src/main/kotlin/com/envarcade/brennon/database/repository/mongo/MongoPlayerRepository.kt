package com.envarcade.brennon.database.repository.mongo

import com.envarcade.brennon.common.model.PlayerData
import com.envarcade.brennon.database.repository.PlayerRepository
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * MongoDB implementation of the PlayerRepository.
 */
class MongoPlayerRepository(database: MongoDatabase) : PlayerRepository {

    private val collection = database.getCollection("players")

    override fun findByUuid(uuid: UUID): CompletableFuture<PlayerData?> {
        return CompletableFuture.supplyAsync {
            val doc = collection.find(Filters.eq("_id", uuid.toString())).first()
            doc?.let { fromDocument(it) }
        }
    }

    override fun findByName(name: String): CompletableFuture<PlayerData?> {
        return CompletableFuture.supplyAsync {
            val doc = collection.find(Filters.regex("name", "^${Regex.escape(name)}$", "i")).first()
            doc?.let { fromDocument(it) }
        }
    }

    override fun save(player: PlayerData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val doc = toDocument(player)
            collection.replaceOne(
                Filters.eq("_id", player.uuid.toString()),
                doc,
                ReplaceOptions().upsert(true)
            )
        }
    }

    override fun delete(uuid: UUID): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            collection.deleteOne(Filters.eq("_id", uuid.toString()))
        }
    }

    override fun exists(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            collection.countDocuments(Filters.eq("_id", uuid.toString())) > 0
        }
    }

    // ============================================================
    // Document Mapping
    // ============================================================

    private fun toDocument(player: PlayerData): Document {
        return Document().apply {
            put("_id", player.uuid.toString())
            put("name", player.name)
            put("primaryRank", player.primaryRank)
            put("ranks", player.ranks.toList())
            put("permissions", player.permissions.toList())
            put("balance", player.balance)
            put("firstJoin", player.firstJoin.toEpochMilli())
            put("lastSeen", player.lastSeen.toEpochMilli())
            put("lastServer", player.lastServer)
            put("ipAddress", player.ipAddress)
            put("playtime", player.playtime)
            put("metadata", Document(player.metadata as Map<String, Any>))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fromDocument(doc: Document): PlayerData {
        return PlayerData(
            uuid = UUID.fromString(doc.getString("_id")),
            name = doc.getString("name"),
            primaryRank = doc.getString("primaryRank") ?: "default",
            ranks = (doc.getList("ranks", String::class.java) ?: listOf("default")).toMutableSet(),
            permissions = (doc.getList("permissions", String::class.java) ?: emptyList()).toMutableSet(),
            balance = doc.getDouble("balance") ?: 0.0,
            firstJoin = Instant.ofEpochMilli(doc.getLong("firstJoin") ?: System.currentTimeMillis()),
            lastSeen = Instant.ofEpochMilli(doc.getLong("lastSeen") ?: System.currentTimeMillis()),
            lastServer = doc.getString("lastServer") ?: "",
            ipAddress = doc.getString("ipAddress") ?: "",
            playtime = doc.getLong("playtime") ?: 0L,
            metadata = (doc.get("metadata", Document::class.java)
                ?.mapValues { it.value.toString() }
                ?.toMutableMap()) ?: mutableMapOf()
        )
    }
}
