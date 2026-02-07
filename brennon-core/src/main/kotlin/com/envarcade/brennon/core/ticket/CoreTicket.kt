package com.envarcade.brennon.core.ticket

import com.envarcade.brennon.api.ticket.Ticket
import com.envarcade.brennon.api.ticket.TicketMessage
import com.envarcade.brennon.api.ticket.TicketPriority
import com.envarcade.brennon.api.ticket.TicketStatus
import com.envarcade.brennon.common.model.TicketData
import java.time.Instant
import java.util.UUID

class CoreTicket(private val data: TicketData) : Ticket {
    override fun getId(): String = data.id
    override fun getCreator(): UUID = data.creator
    override fun getCreatorName(): String = data.creatorName
    override fun getAssignee(): UUID? = data.assignee
    override fun getSubject(): String = data.subject
    override fun getStatus(): TicketStatus = data.status
    override fun getPriority(): TicketPriority = data.priority
    override fun getServer(): String = data.server
    override fun getCreatedAt(): Instant = data.createdAt
    override fun getUpdatedAt(): Instant = data.updatedAt
    override fun getClosedAt(): Instant? = data.closedAt
    override fun getMessages(): List<TicketMessage> = data.messages.map { CoreTicketMessage(it) }
}
