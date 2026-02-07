package com.envarcade.brennon.database.repository.mongo

import com.envarcade.brennon.common.model.RankData
import com.envarcade.brennon.database.repository.RankRepository
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import java.util.concurrent.CompletableFuture

/**
 * MongoDB implementation of the RankRepository.
 */
class MongoRankRepository(database: MongoDatabase) : RankRepository {

    private val collection = database.getCollection("ranks")

    override fun findById(id: String): CompletableFuture<RankData?> {
        return CompletableFuture.supplyAsync {
            val doc = collection.find(Filters.eq("_id", id)).first()
            doc?.let { fromDocument(it) }
        }
    }

    override fun findAll(): CompletableFuture<List<RankData>> {
        return CompletableFuture.supplyAsync {
            collection.find().map { fromDocument(it) }.toList()
        }
    }

    override fun findDefault(): CompletableFuture<RankData?> {
        return CompletableFuture.supplyAsync {
            val doc = collection.find(Filters.eq("isDefault", true)).first()
            doc?.let { fromDocument(it) }
        }
    }

    override fun save(rank: RankData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val doc = toDocument(rank)
            collection.replaceOne(
                Filters.eq("_id", rank.id),
                doc,
                ReplaceOptions().upsert(true)
            )
        }
    }

    override fun delete(id: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            collection.deleteOne(Filters.eq("_id", id))
        }
    }

    override fun exists(id: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            collection.countDocuments(Filters.eq("_id", id)) > 0
        }
    }

    private fun toDocument(rank: RankData): Document {
        return Document().apply {
            put("_id", rank.id)
            put("displayName", rank.displayName)
            put("prefix", rank.prefix)
            put("suffix", rank.suffix)
            put("weight", rank.weight)
            put("permissions", rank.permissions.toList())
            put("inheritance", rank.inheritance.toList())
            put("isDefault", rank.isDefault)
            put("isStaff", rank.isStaff)
            put("metadata", Document(rank.metadata as Map<String, Any>))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fromDocument(doc: Document): RankData {
        return RankData(
            id = doc.getString("_id"),
            displayName = doc.getString("displayName") ?: doc.getString("_id"),
            prefix = doc.getString("prefix") ?: "",
            suffix = doc.getString("suffix") ?: "",
            weight = doc.getInteger("weight") ?: 0,
            permissions = (doc.getList("permissions", String::class.java) ?: emptyList()).toMutableSet(),
            inheritance = (doc.getList("inheritance", String::class.java) ?: emptyList()).toMutableSet(),
            isDefault = doc.getBoolean("isDefault") ?: false,
            isStaff = doc.getBoolean("isStaff") ?: false,
            metadata = (doc.get("metadata", Document::class.java)
                ?.mapValues { it.value.toString() }
                ?.toMutableMap()) ?: mutableMapOf()
        )
    }
}
