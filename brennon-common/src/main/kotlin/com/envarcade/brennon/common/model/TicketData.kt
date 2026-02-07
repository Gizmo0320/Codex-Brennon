package com.envarcade.brennon.common.model

import com.envarcade.brennon.api.ticket.TicketPriority
import com.envarcade.brennon.api.ticket.TicketStatus
import java.time.Instant
import java.util.UUID

data class TicketData(
    val id: String,
    val creator: UUID,
    val creatorName: String,
    var assignee: UUID? = null,
    val subject: String,
    var status: TicketStatus = TicketStatus.OPEN,
    var priority: TicketPriority = TicketPriority.NORMAL,
    val server: String,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
    var closedAt: Instant? = null,
    val messages: MutableList<TicketMessageData> = mutableListOf(),
    val networkId: String? = null
)
