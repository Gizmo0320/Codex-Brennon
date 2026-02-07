package com.envarcade.brennon.database.repository

import com.envarcade.brennon.api.ticket.TicketStatus
import com.envarcade.brennon.common.model.TicketData
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface TicketRepository {
    fun findById(id: String): CompletableFuture<TicketData?>
    fun findByStatus(status: TicketStatus): CompletableFuture<List<TicketData>>
    fun findByCreator(uuid: UUID): CompletableFuture<List<TicketData>>
    fun findByAssignee(uuid: UUID): CompletableFuture<List<TicketData>>
    fun findOpen(): CompletableFuture<List<TicketData>>
    fun save(ticket: TicketData): CompletableFuture<Void>
    fun delete(id: String): CompletableFuture<Void>
    fun getNextId(): CompletableFuture<Int>
}
