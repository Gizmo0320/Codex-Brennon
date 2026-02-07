package com.envarcade.brennon.database.repository.sql

import com.envarcade.brennon.api.ticket.TicketPriority
import com.envarcade.brennon.api.ticket.TicketStatus
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.common.model.TicketData
import com.envarcade.brennon.common.model.TicketMessageData
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import com.envarcade.brennon.database.repository.TicketRepository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * SQL implementation of the TicketRepository.
 * Network-aware: filters by network_id when sharing mode is NETWORK.
 */
class SQLTicketRepository(
    private val driver: SQLDatabaseDriver,
    private val networkContext: NetworkContext
) : TicketRepository {

    private val networkId: String? = networkContext.effectiveNetworkId(networkContext.sharing.tickets)
    private val isNetworkScoped: Boolean = networkId != null

    private val upsertSQL: String = if (driver.isPostgres) {
        """
        INSERT INTO brennon_tickets (id, creator, creator_name, assignee, subject, status, priority, server, created_at, updated_at, closed_at, network_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            assignee = EXCLUDED.assignee,
            status = EXCLUDED.status,
            priority = EXCLUDED.priority,
            updated_at = EXCLUDED.updated_at,
            closed_at = EXCLUDED.closed_at
        """.trimIndent()
    } else {
        """
        INSERT INTO brennon_tickets (id, creator, creator_name, assignee, subject, status, priority, server, created_at, updated_at, closed_at, network_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            assignee = VALUES(assignee),
            status = VALUES(status),
            priority = VALUES(priority),
            updated_at = VALUES(updated_at),
            closed_at = VALUES(closed_at)
        """.trimIndent()
    }

    override fun findById(id: String): CompletableFuture<TicketData?> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_tickets WHERE id = ?").use { stmt ->
                    stmt.setString(1, id)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val ticket = fromResultSet(rs)
                        loadMessages(ticket)
                    } else null
                }
            }
        }
    }

    override fun findByStatus(status: TicketStatus): CompletableFuture<List<TicketData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_tickets WHERE status = ?")
                    if (isNetworkScoped) append(" AND network_id = ?")
                    append(" ORDER BY created_at DESC")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, status.name)
                    if (isNetworkScoped) stmt.setString(2, networkId!!)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<TicketData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list.forEach { loadMessages(it) }
                    list
                }
            }
        }
    }

    override fun findByCreator(uuid: UUID): CompletableFuture<List<TicketData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_tickets WHERE creator = ?")
                    if (isNetworkScoped) append(" AND network_id = ?")
                    append(" ORDER BY created_at DESC")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    if (isNetworkScoped) stmt.setString(2, networkId!!)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<TicketData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list.forEach { loadMessages(it) }
                    list
                }
            }
        }
    }

    override fun findByAssignee(uuid: UUID): CompletableFuture<List<TicketData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_tickets WHERE assignee = ?")
                    if (isNetworkScoped) append(" AND network_id = ?")
                    append(" ORDER BY created_at DESC")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    if (isNetworkScoped) stmt.setString(2, networkId!!)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<TicketData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list.forEach { loadMessages(it) }
                    list
                }
            }
        }
    }

    override fun findOpen(): CompletableFuture<List<TicketData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_tickets WHERE status IN ('OPEN', 'IN_PROGRESS', 'WAITING_RESPONSE')")
                    if (isNetworkScoped) append(" AND network_id = ?")
                    append(" ORDER BY created_at DESC")
                }
                conn.prepareStatement(sql).use { stmt ->
                    if (isNetworkScoped) stmt.setString(1, networkId!!)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<TicketData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list.forEach { loadMessages(it) }
                    list
                }
            }
        }
    }

    override fun save(ticket: TicketData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                // Save ticket
                conn.prepareStatement(upsertSQL).use { stmt ->
                    stmt.setString(1, ticket.id)
                    stmt.setString(2, ticket.creator.toString())
                    stmt.setString(3, ticket.creatorName)
                    stmt.setString(4, ticket.assignee?.toString())
                    stmt.setString(5, ticket.subject)
                    stmt.setString(6, ticket.status.name)
                    stmt.setString(7, ticket.priority.name)
                    stmt.setString(8, ticket.server)
                    stmt.setLong(9, ticket.createdAt.toEpochMilli())
                    stmt.setLong(10, ticket.updatedAt.toEpochMilli())
                    if (ticket.closedAt != null) {
                        stmt.setLong(11, ticket.closedAt!!.toEpochMilli())
                    } else {
                        stmt.setNull(11, java.sql.Types.BIGINT)
                    }
                    stmt.setString(12, ticket.networkId ?: networkId)
                    stmt.executeUpdate()
                }

                // Replace messages (delete + reinsert)
                conn.prepareStatement("DELETE FROM brennon_ticket_messages WHERE ticket_id = ?").use { stmt ->
                    stmt.setString(1, ticket.id)
                    stmt.executeUpdate()
                }

                if (ticket.messages.isNotEmpty()) {
                    conn.prepareStatement(
                        "INSERT INTO brennon_ticket_messages (ticket_id, author, author_name, content, timestamp, is_staff) VALUES (?, ?, ?, ?, ?, ?)"
                    ).use { stmt ->
                        for (msg in ticket.messages) {
                            stmt.setString(1, ticket.id)
                            stmt.setString(2, msg.author.toString())
                            stmt.setString(3, msg.authorName)
                            stmt.setString(4, msg.content)
                            stmt.setLong(5, msg.timestamp.toEpochMilli())
                            stmt.setBoolean(6, msg.isStaffMessage)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                }
            }
        }
    }

    override fun delete(id: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM brennon_ticket_messages WHERE ticket_id = ?").use { stmt ->
                    stmt.setString(1, id)
                    stmt.executeUpdate()
                }
                conn.prepareStatement("DELETE FROM brennon_tickets WHERE id = ?").use { stmt ->
                    stmt.setString(1, id)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun getNextId(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT COALESCE(MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)), 0) + 1 AS next_id FROM brennon_tickets").use { stmt ->
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt("next_id") else 1
                }
            }
        }
    }

    private fun loadMessages(ticket: TicketData): TicketData {
        driver.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM brennon_ticket_messages WHERE ticket_id = ? ORDER BY timestamp ASC").use { stmt ->
                stmt.setString(1, ticket.id)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    ticket.messages.add(
                        TicketMessageData(
                            author = UUID.fromString(rs.getString("author")),
                            authorName = rs.getString("author_name"),
                            content = rs.getString("content"),
                            timestamp = Instant.ofEpochMilli(rs.getLong("timestamp")),
                            isStaffMessage = rs.getBoolean("is_staff")
                        )
                    )
                }
            }
        }
        return ticket
    }

    private fun fromResultSet(rs: ResultSet): TicketData {
        return TicketData(
            id = rs.getString("id"),
            creator = UUID.fromString(rs.getString("creator")),
            creatorName = rs.getString("creator_name"),
            assignee = rs.getString("assignee")?.let { UUID.fromString(it) },
            subject = rs.getString("subject"),
            status = TicketStatus.valueOf(rs.getString("status")),
            priority = TicketPriority.valueOf(rs.getString("priority")),
            server = rs.getString("server"),
            createdAt = Instant.ofEpochMilli(rs.getLong("created_at")),
            updatedAt = Instant.ofEpochMilli(rs.getLong("updated_at")),
            closedAt = rs.getLong("closed_at").let { if (rs.wasNull()) null else Instant.ofEpochMilli(it) },
            networkId = rs.getString("network_id")
        )
    }
}
