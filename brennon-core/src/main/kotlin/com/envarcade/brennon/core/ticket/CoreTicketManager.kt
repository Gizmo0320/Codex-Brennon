package com.envarcade.brennon.core.ticket

import com.envarcade.brennon.api.ticket.*
import com.envarcade.brennon.common.model.TicketData
import com.envarcade.brennon.common.model.TicketMessageData
import com.envarcade.brennon.core.event.*
import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.packet.Packet
import com.envarcade.brennon.messaging.packet.TicketPacket
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import java.time.Instant
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture

class CoreTicketManager(
    private val database: DatabaseManager,
    private val messaging: RedisMessagingService,
    private val eventBus: CoreEventBus,
    private val networkId: String? = null
) : TicketManager {

    override fun createTicket(
        creator: UUID,
        creatorName: String,
        subject: String,
        message: String,
        server: String
    ): CompletableFuture<Ticket> {
        return database.tickets.getNextId().thenCompose { nextId ->
            val ticketId = "T-$nextId"
            val ticketData = TicketData(
                id = ticketId,
                creator = creator,
                creatorName = creatorName,
                subject = subject,
                server = server,
                messages = mutableListOf(
                    TicketMessageData(
                        author = creator,
                        authorName = creatorName,
                        content = message
                    )
                ),
                networkId = networkId
            )

            database.tickets.save(ticketData).thenApply {
                eventBus.publish(TicketCreateEvent(ticketId, creator, creatorName, subject))

                val packet = TicketPacket(
                    ticketId = ticketId,
                    action = "create",
                    actorUuid = creator.toString(),
                    actorName = creatorName,
                    extra = subject
                )
                messaging.publish(Channels.TICKET_CREATE, Packet.serialize(packet))
                messaging.publish(Channels.STAFF_ALERT, "[Ticket] New ticket $ticketId by $creatorName: $subject")

                CoreTicket(ticketData)
            }
        }
    }

    override fun getTicket(id: String): CompletableFuture<Optional<Ticket>> {
        return database.tickets.findById(id).thenApply { data ->
            Optional.ofNullable(data?.let { CoreTicket(it) })
        }
    }

    override fun getOpenTickets(): CompletableFuture<List<Ticket>> {
        return database.tickets.findOpen().thenApply { list ->
            list.map { CoreTicket(it) }
        }
    }

    override fun getPlayerTickets(player: UUID): CompletableFuture<List<Ticket>> {
        return database.tickets.findByCreator(player).thenApply { list ->
            list.map { CoreTicket(it) }
        }
    }

    override fun getAssignedTickets(staff: UUID): CompletableFuture<List<Ticket>> {
        return database.tickets.findByAssignee(staff).thenApply { list ->
            list.map { CoreTicket(it) }
        }
    }

    override fun addReply(
        ticketId: String,
        author: UUID,
        authorName: String,
        message: String,
        isStaff: Boolean
    ): CompletableFuture<Void> {
        return database.tickets.findById(ticketId).thenCompose { data ->
            if (data == null) return@thenCompose CompletableFuture.completedFuture(null)

            data.messages.add(
                TicketMessageData(
                    author = author,
                    authorName = authorName,
                    content = message,
                    isStaffMessage = isStaff
                )
            )
            data.updatedAt = Instant.now()

            database.tickets.save(data).thenRun {
                eventBus.publish(TicketReplyEvent(ticketId, author, authorName, isStaff))

                val packet = TicketPacket(ticketId, "reply", author.toString(), authorName)
                messaging.publish(Channels.TICKET_REPLY, Packet.serialize(packet))
            }
        }
    }

    override fun assignTicket(ticketId: String, staff: UUID): CompletableFuture<Void> {
        return database.tickets.findById(ticketId).thenCompose { data ->
            if (data == null) return@thenCompose CompletableFuture.completedFuture(null)

            data.assignee = staff
            data.status = TicketStatus.IN_PROGRESS
            data.updatedAt = Instant.now()

            database.tickets.save(data).thenRun {
                eventBus.publish(TicketAssignEvent(ticketId, staff, null))

                val packet = TicketPacket(ticketId, "assign", staff.toString(), null)
                messaging.publish(Channels.TICKET_UPDATE, Packet.serialize(packet))
            }
        }
    }

    override fun setStatus(ticketId: String, status: TicketStatus): CompletableFuture<Void> {
        return database.tickets.findById(ticketId).thenCompose { data ->
            if (data == null) return@thenCompose CompletableFuture.completedFuture(null)

            val oldStatus = data.status
            data.status = status
            data.updatedAt = Instant.now()

            database.tickets.save(data).thenRun {
                eventBus.publish(TicketStatusChangeEvent(ticketId, oldStatus, status, null))
            }
        }
    }

    override fun setPriority(ticketId: String, priority: TicketPriority): CompletableFuture<Void> {
        return database.tickets.findById(ticketId).thenCompose { data ->
            if (data == null) return@thenCompose CompletableFuture.completedFuture(null)

            data.priority = priority
            data.updatedAt = Instant.now()

            database.tickets.save(data)
        }
    }

    override fun closeTicket(ticketId: String, closedBy: UUID): CompletableFuture<Void> {
        return database.tickets.findById(ticketId).thenCompose { data ->
            if (data == null) return@thenCompose CompletableFuture.completedFuture(null)

            val oldStatus = data.status
            data.status = TicketStatus.CLOSED
            data.closedAt = Instant.now()
            data.updatedAt = Instant.now()

            database.tickets.save(data).thenRun {
                eventBus.publish(TicketStatusChangeEvent(ticketId, oldStatus, TicketStatus.CLOSED, closedBy))

                val packet = TicketPacket(ticketId, "close", closedBy.toString(), null)
                messaging.publish(Channels.TICKET_UPDATE, Packet.serialize(packet))
            }
        }
    }
}
