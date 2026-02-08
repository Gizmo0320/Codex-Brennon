package com.envarcade.brennon.database.repository.mongo

import com.envarcade.brennon.api.punishment.PunishmentType
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.common.model.PunishmentData
import com.envarcade.brennon.database.repository.PunishmentRepository
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import org.bson.conversions.Bson
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * MongoDB implementation of the PunishmentRepository.
 * Network-aware: filters by networkId when sharing mode is NETWORK.
 */
class MongoPunishmentRepository(
    database: MongoDatabase,
    private val networkContext: NetworkContext
) : PunishmentRepository {

    private val collection = database.getCollection("punishments")
    private val networkId: String? = networkContext.effectiveNetworkId(networkContext.sharing.punishments)

    private fun networkFilter(): Bson? =
        if (networkId != null) Filters.eq("networkId", networkId) else null

    private fun withNetworkFilter(vararg filters: Bson): Bson {
        val all = filters.toMutableList()
        networkFilter()?.let { all.add(it) }
        return Filters.and(all)
    }

    override fun findById(id: String): CompletableFuture<PunishmentData?> {
        return CompletableFuture.supplyAsync {
            val doc = collection.find(Filters.eq("_id", id)).first()
            doc?.let { fromDocument(it) }
        }
    }

    override fun findByTarget(uuid: UUID): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            collection.find(withNetworkFilter(Filters.eq("target", uuid.toString())))
                .sort(Document("issuedAt", -1))
                .map { fromDocument(it) }
                .toList()
        }
    }

    override fun findActiveByTarget(uuid: UUID): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            collection.find(
                withNetworkFilter(
                    Filters.eq("target", uuid.toString()),
                    Filters.eq("active", true)
                )
            ).map { fromDocument(it) }.toList()
        }
    }

    override fun findActiveByTargetAndType(uuid: UUID, type: PunishmentType): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            collection.find(
                withNetworkFilter(
                    Filters.eq("target", uuid.toString()),
                    Filters.eq("type", type.name),
                    Filters.eq("active", true)
                )
            ).map { fromDocument(it) }.toList()
        }
    }

    override fun findActiveByIp(ip: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            collection.find(
                withNetworkFilter(
                    Filters.eq("targetIp", ip),
                    Filters.eq("type", "IP_BAN"),
                    Filters.eq("active", true)
                )
            ).map { fromDocument(it) }.toList()
        }
    }

    override fun findAllByType(type: PunishmentType, limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            collection.find(
                withNetworkFilter(Filters.eq("type", type.name))
            )
                .sort(Document("issuedAt", -1))
                .skip(offset)
                .limit(limit)
                .map { fromDocument(it) }
                .toList()
        }
    }

    override fun countByType(type: PunishmentType): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            collection.countDocuments(
                withNetworkFilter(Filters.eq("type", type.name))
            ).toInt()
        }
    }

    override fun save(punishment: PunishmentData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val doc = toDocument(punishment)
            collection.replaceOne(
                Filters.eq("_id", punishment.id),
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

    private fun toDocument(punishment: PunishmentData): Document {
        return Document().apply {
            put("_id", punishment.id)
            put("target", punishment.target.toString())
            put("issuer", punishment.issuer?.toString())
            put("type", punishment.type.name)
            put("reason", punishment.reason)
            put("issuedAt", punishment.issuedAt.toEpochMilli())
            put("expiresAt", punishment.expiresAt?.toEpochMilli())
            put("active", punishment.active)
            put("revokedBy", punishment.revokedBy?.toString())
            put("revokedAt", punishment.revokedAt?.toEpochMilli())
            put("revokeReason", punishment.revokeReason)
            put("networkId", punishment.networkId ?: networkId)
            put("targetIp", punishment.targetIp)
        }
    }

    private fun fromDocument(doc: Document): PunishmentData {
        return PunishmentData(
            id = doc.getString("_id"),
            target = UUID.fromString(doc.getString("target")),
            issuer = doc.getString("issuer")?.let { UUID.fromString(it) },
            type = PunishmentType.valueOf(doc.getString("type")),
            reason = doc.getString("reason"),
            issuedAt = Instant.ofEpochMilli(doc.getLong("issuedAt") ?: System.currentTimeMillis()),
            expiresAt = doc.getLong("expiresAt")?.let { Instant.ofEpochMilli(it) },
            active = doc.getBoolean("active") ?: true,
            revokedBy = doc.getString("revokedBy")?.let { UUID.fromString(it) },
            revokedAt = doc.getLong("revokedAt")?.let { Instant.ofEpochMilli(it) },
            revokeReason = doc.getString("revokeReason"),
            networkId = doc.getString("networkId"),
            targetIp = doc.getString("targetIp")
        )
    }
}
