package com.envarcade.brennon.core.ticket

import com.envarcade.brennon.api.ticket.TicketMessage
import com.envarcade.brennon.common.model.TicketMessageData
import java.time.Instant
import java.util.UUID

class CoreTicketMessage(private val data: TicketMessageData) : TicketMessage {
    override fun getAuthor(): UUID = data.author
    override fun getAuthorName(): String = data.authorName
    override fun getContent(): String = data.content
    override fun getTimestamp(): Instant = data.timestamp
    override fun isStaffMessage(): Boolean = data.isStaffMessage
}
