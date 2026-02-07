package com.envarcade.brennon.database.repository.mongo

import com.envarcade.brennon.api.ticket.TicketPriority
import com.envarcade.brennon.api.ticket.TicketStatus
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.common.model.TicketData
import com.envarcade.brennon.common.model.TicketMessageData
import com.envarcade.brennon.database.repository.TicketRepository
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.conversions.Bson
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * MongoDB implementation of the TicketRepository.
 * Network-aware: filters by networkId when sharing mode is NETWORK.
 */
class MongoTicketRepository(
    database: MongoDatabase,
    private val networkContext: NetworkContext
) : TicketRepository {

    private val collection = database.getCollection("tickets")
    private val counters = database.getCollection("counters")
    private val networkId: String? = networkContext.effectiveNetworkId(networkContext.sharing.tickets)

    private fun networkFilter(): Bson? =
        if (networkId != null) Filters.eq("networkId", networkId) else null

    private fun withNetworkFilter(vararg filters: Bson): Bson {
        val all = filters.toMutableList()
        networkFilter()?.let { all.add(it) }
        return Filters.and(all)
    }

    override fun findById(id: String): CompletableFuture<TicketData?> {
        return CompletableFuture.supplyAsync {
            val doc = collection.find(Filters.eq("_id", id)).first()
            doc?.let { fromDocument(it) }
        }
    }

    override fun findByStatus(status: TicketStatus): CompletableFuture<List<TicketData>> {
        return CompletableFuture.supplyAsync {
            collection.find(withNetworkFilter(Filters.eq("status", status.name)))
                .sort(Document("createdAt", -1))
                .map { fromDocument(it) }
                .toList()
        }
    }

    override fun findByCreator(uuid: UUID): CompletableFuture<List<TicketData>> {
        return CompletableFuture.supplyAsync {
            collection.find(withNetworkFilter(Filters.eq("creator", uuid.toString())))
                .sort(Document("createdAt", -1))
                .map { fromDocument(it) }
                .toList()
        }
    }

    override fun findByAssignee(uuid: UUID): CompletableFuture<List<TicketData>> {
        return CompletableFuture.supplyAsync {
            collection.find(withNetworkFilter(Filters.eq("assignee", uuid.toString())))
                .sort(Document("createdAt", -1))
                .map { fromDocument(it) }
                .toList()
        }
    }

    override fun findOpen(): CompletableFuture<List<TicketData>> {
        return CompletableFuture.supplyAsync {
            collection.find(
                withNetworkFilter(
                    Filters.`in`("status", TicketStatus.OPEN.name, TicketStatus.IN_PROGRESS.name, TicketStatus.WAITING_RESPONSE.name)
                )
            ).sort(Document("createdAt", -1))
                .map { fromDocument(it) }
                .toList()
        }
    }

    override fun save(ticket: TicketData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            collection.replaceOne(
                Filters.eq("_id", ticket.id),
                toDocument(ticket),
                ReplaceOptions().upsert(true)
            )
        }
    }

    override fun delete(id: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            collection.deleteOne(Filters.eq("_id", id))
        }
    }

    override fun getNextId(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            val result = counters.findOneAndUpdate(
                Filters.eq("_id", "ticket_counter"),
                Updates.inc("seq", 1),
                FindOneAndUpdateOptions()
                    .upsert(true)
                    .returnDocument(ReturnDocument.AFTER)
            )
            result?.getInteger("seq") ?: 1
        }
    }

    private fun toDocument(ticket: TicketData): Document {
        return Document().apply {
            put("_id", ticket.id)
            put("creator", ticket.creator.toString())
            put("creatorName", ticket.creatorName)
            put("assignee", ticket.assignee?.toString())
            put("subject", ticket.subject)
            put("status", ticket.status.name)
            put("priority", ticket.priority.name)
            put("server", ticket.server)
            put("createdAt", ticket.createdAt.toEpochMilli())
            put("updatedAt", ticket.updatedAt.toEpochMilli())
            put("closedAt", ticket.closedAt?.toEpochMilli())
            put("messages", ticket.messages.map { messageToDocument(it) })
            put("networkId", ticket.networkId ?: networkId)
        }
    }

    private fun messageToDocument(msg: TicketMessageData): Document {
        return Document().apply {
            put("author", msg.author.toString())
            put("authorName", msg.authorName)
            put("content", msg.content)
            put("timestamp", msg.timestamp.toEpochMilli())
            put("isStaffMessage", msg.isStaffMessage)
        }
    }

    private fun fromDocument(doc: Document): TicketData {
        val messages = (doc.getList("messages", Document::class.java) ?: emptyList())
            .map { messageFromDocument(it) }
            .toMutableList()

        return TicketData(
            id = doc.getString("_id"),
            creator = UUID.fromString(doc.getString("creator")),
            creatorName = doc.getString("creatorName"),
            assignee = doc.getString("assignee")?.let { UUID.fromString(it) },
            subject = doc.getString("subject"),
            status = TicketStatus.valueOf(doc.getString("status")),
            priority = TicketPriority.valueOf(doc.getString("priority")),
            server = doc.getString("server"),
            createdAt = Instant.ofEpochMilli(doc.getLong("createdAt") ?: System.currentTimeMillis()),
            updatedAt = Instant.ofEpochMilli(doc.getLong("updatedAt") ?: System.currentTimeMillis()),
            closedAt = doc.getLong("closedAt")?.let { Instant.ofEpochMilli(it) },
            messages = messages,
            networkId = doc.getString("networkId")
        )
    }

    private fun messageFromDocument(doc: Document): TicketMessageData {
        return TicketMessageData(
            author = UUID.fromString(doc.getString("author")),
            authorName = doc.getString("authorName"),
            content = doc.getString("content"),
            timestamp = Instant.ofEpochMilli(doc.getLong("timestamp") ?: System.currentTimeMillis()),
            isStaffMessage = doc.getBoolean("isStaffMessage") ?: false
        )
    }
}
