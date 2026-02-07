package com.envarcade.brennon.core.network

import com.envarcade.brennon.common.model.PunishmentData
import com.envarcade.brennon.common.model.TicketData
import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Admin utility for querying data across ALL networks.
 *
 * Bypasses the normal network_id filter to provide a global view.
 * Only accessible with `brennon.admin.crossnetwork` permission.
 */
class CrossNetworkService(
    private val database: DatabaseManager
) {

    /**
     * Gets all punishments for a player across all networks.
     */
    fun getAllPunishments(uuid: UUID): CompletableFuture<List<PunishmentData>> {
        val driver = database.driver
        if (driver !is SQLDatabaseDriver) {
            // For Mongo, fall back to the normal repo (no network filter bypass yet)
            return database.punishments.findByTarget(uuid)
        }

        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT * FROM brennon_punishments WHERE target = ? ORDER BY issued_at DESC"
                ).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<PunishmentData>()
                    while (rs.next()) {
                        list.add(PunishmentData(
                            id = rs.getString("id"),
                            target = UUID.fromString(rs.getString("target")),
                            issuer = rs.getString("issuer")?.let { UUID.fromString(it) },
                            type = com.envarcade.brennon.api.punishment.PunishmentType.valueOf(rs.getString("type")),
                            reason = rs.getString("reason"),
                            issuedAt = Instant.ofEpochMilli(rs.getLong("issued_at")),
                            expiresAt = rs.getLong("expires_at").let { if (rs.wasNull()) null else Instant.ofEpochMilli(it) },
                            active = rs.getBoolean("active"),
                            revokedBy = rs.getString("revoked_by")?.let { UUID.fromString(it) },
                            revokedAt = rs.getLong("revoked_at").let { if (rs.wasNull()) null else Instant.ofEpochMilli(it) },
                            revokeReason = rs.getString("revoke_reason"),
                            networkId = rs.getString("network_id")
                        ))
                    }
                    list
                }
            }
        }
    }

    /**
     * Gets all tickets created by a player across all networks.
     */
    fun getAllTickets(uuid: UUID): CompletableFuture<List<TicketData>> {
        val driver = database.driver
        if (driver !is SQLDatabaseDriver) {
            return database.tickets.findByCreator(uuid)
        }

        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT * FROM brennon_tickets WHERE creator = ? ORDER BY created_at DESC"
                ).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<TicketData>()
                    while (rs.next()) {
                        list.add(TicketData(
                            id = rs.getString("id"),
                            creator = UUID.fromString(rs.getString("creator")),
                            creatorName = rs.getString("creator_name"),
                            subject = rs.getString("subject"),
                            server = rs.getString("server"),
                            status = com.envarcade.brennon.api.ticket.TicketStatus.valueOf(rs.getString("status")),
                            priority = com.envarcade.brennon.api.ticket.TicketPriority.valueOf(rs.getString("priority")),
                            assignee = rs.getString("assignee")?.let { UUID.fromString(it) },
                            createdAt = Instant.ofEpochMilli(rs.getLong("created_at")),
                            updatedAt = Instant.ofEpochMilli(rs.getLong("updated_at")),
                            closedAt = rs.getLong("closed_at").let { if (rs.wasNull()) null else Instant.ofEpochMilli(it) },
                            networkId = rs.getString("network_id")
                        ))
                    }
                    list
                }
            }
        }
    }

    /**
     * Gets stats for a player on a specific network.
     */
    fun getStatsForNetwork(uuid: UUID, networkId: String): CompletableFuture<Map<String, Double>> {
        val driver = database.driver
        if (driver !is SQLDatabaseDriver) {
            return database.stats.getAllStats(uuid)
        }

        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT stat_id, value FROM brennon_stats WHERE player_uuid = ? AND network_id = ?"
                ).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, networkId)
                    val rs = stmt.executeQuery()
                    val stats = mutableMapOf<String, Double>()
                    while (rs.next()) {
                        stats[rs.getString("stat_id")] = rs.getDouble("value")
                    }
                    stats
                }
            }
        }
    }
}
