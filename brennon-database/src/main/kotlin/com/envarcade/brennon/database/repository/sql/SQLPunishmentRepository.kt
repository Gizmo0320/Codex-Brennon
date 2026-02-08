package com.envarcade.brennon.database.repository.sql

import com.envarcade.brennon.api.punishment.PunishmentType
import com.envarcade.brennon.common.config.DataSharingMode
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.common.model.PunishmentData
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import com.envarcade.brennon.database.repository.PunishmentRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * SQL implementation of the PunishmentRepository.
 * Supports MySQL, MariaDB, and PostgreSQL.
 * Network-aware: filters by network_id when sharing mode is NETWORK.
 */
class SQLPunishmentRepository(
    private val driver: SQLDatabaseDriver,
    private val networkContext: NetworkContext
) : PunishmentRepository {

    private val networkId: String? = networkContext.effectiveNetworkId(networkContext.sharing.punishments)
    private val isNetworkScoped: Boolean = networkId != null

    private val upsertSQL: String = if (driver.isPostgres) {
        """
        INSERT INTO brennon_punishments (id, target, issuer, type, reason, issued_at, expires_at, active, revoked_by, revoked_at, revoke_reason, network_id, target_ip)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            active = EXCLUDED.active,
            revoked_by = EXCLUDED.revoked_by,
            revoked_at = EXCLUDED.revoked_at,
            revoke_reason = EXCLUDED.revoke_reason
        """.trimIndent()
    } else {
        """
        INSERT INTO brennon_punishments (id, target, issuer, type, reason, issued_at, expires_at, active, revoked_by, revoked_at, revoke_reason, network_id, target_ip)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            active = VALUES(active),
            revoked_by = VALUES(revoked_by),
            revoked_at = VALUES(revoked_at),
            revoke_reason = VALUES(revoke_reason)
        """.trimIndent()
    }

    override fun findById(id: String): CompletableFuture<PunishmentData?> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_punishments WHERE id = ?").use { stmt ->
                    stmt.setString(1, id)
                    val rs = stmt.executeQuery()
                    if (rs.next()) fromResultSet(rs) else null
                }
            }
        }
    }

    override fun findByTarget(uuid: UUID): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_punishments WHERE target = ?")
                    if (isNetworkScoped) append(" AND network_id = ?")
                    append(" ORDER BY issued_at DESC")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    if (isNetworkScoped) stmt.setString(2, networkId!!)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<PunishmentData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list
                }
            }
        }
    }

    override fun findActiveByTarget(uuid: UUID): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_punishments WHERE target = ? AND active = TRUE")
                    if (isNetworkScoped) append(" AND network_id = ?")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    if (isNetworkScoped) stmt.setString(2, networkId!!)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<PunishmentData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list
                }
            }
        }
    }

    override fun findActiveByTargetAndType(uuid: UUID, type: PunishmentType): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_punishments WHERE target = ? AND type = ? AND active = TRUE")
                    if (isNetworkScoped) append(" AND network_id = ?")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, type.name)
                    if (isNetworkScoped) stmt.setString(3, networkId!!)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<PunishmentData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list
                }
            }
        }
    }

    override fun findActiveByIp(ip: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_punishments WHERE target_ip = ? AND type = 'IP_BAN' AND active = TRUE")
                    if (isNetworkScoped) append(" AND network_id = ?")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, ip)
                    if (isNetworkScoped) stmt.setString(2, networkId!!)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<PunishmentData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list
                }
            }
        }
    }

    override fun findAllByType(type: PunishmentType, limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT * FROM brennon_punishments WHERE type = ?")
                    if (isNetworkScoped) append(" AND network_id = ?")
                    append(" ORDER BY issued_at DESC LIMIT ? OFFSET ?")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, type.name)
                    if (isNetworkScoped) {
                        stmt.setString(2, networkId!!)
                        stmt.setInt(3, limit)
                        stmt.setInt(4, offset)
                    } else {
                        stmt.setInt(2, limit)
                        stmt.setInt(3, offset)
                    }
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<PunishmentData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list
                }
            }
        }
    }

    override fun countByType(type: PunishmentType): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                val sql = buildString {
                    append("SELECT COUNT(*) FROM brennon_punishments WHERE type = ?")
                    if (isNetworkScoped) append(" AND network_id = ?")
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, type.name)
                    if (isNetworkScoped) stmt.setString(2, networkId!!)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    override fun save(punishment: PunishmentData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement(upsertSQL).use { stmt ->
                    stmt.setString(1, punishment.id)
                    stmt.setString(2, punishment.target.toString())
                    stmt.setString(3, punishment.issuer?.toString())
                    stmt.setString(4, punishment.type.name)
                    stmt.setString(5, punishment.reason)
                    stmt.setLong(6, punishment.issuedAt.toEpochMilli())
                    if (punishment.expiresAt != null) {
                        stmt.setLong(7, punishment.expiresAt!!.toEpochMilli())
                    } else {
                        stmt.setNull(7, java.sql.Types.BIGINT)
                    }
                    stmt.setBoolean(8, punishment.active)
                    stmt.setString(9, punishment.revokedBy?.toString())
                    if (punishment.revokedAt != null) {
                        stmt.setLong(10, punishment.revokedAt!!.toEpochMilli())
                    } else {
                        stmt.setNull(10, java.sql.Types.BIGINT)
                    }
                    stmt.setString(11, punishment.revokeReason)
                    stmt.setString(12, punishment.networkId ?: networkId)
                    stmt.setString(13, punishment.targetIp)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun delete(id: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM brennon_punishments WHERE id = ?").use { stmt ->
                    stmt.setString(1, id)
                    stmt.executeUpdate()
                }
            }
        }
    }

    private fun fromResultSet(rs: java.sql.ResultSet): PunishmentData {
        return PunishmentData(
            id = rs.getString("id"),
            target = UUID.fromString(rs.getString("target")),
            issuer = rs.getString("issuer")?.let { UUID.fromString(it) },
            type = PunishmentType.valueOf(rs.getString("type")),
            reason = rs.getString("reason"),
            issuedAt = Instant.ofEpochMilli(rs.getLong("issued_at")),
            expiresAt = rs.getLong("expires_at").let { if (rs.wasNull()) null else Instant.ofEpochMilli(it) },
            active = rs.getBoolean("active"),
            revokedBy = rs.getString("revoked_by")?.let { UUID.fromString(it) },
            revokedAt = rs.getLong("revoked_at").let { if (rs.wasNull()) null else Instant.ofEpochMilli(it) },
            revokeReason = rs.getString("revoke_reason"),
            networkId = rs.getString("network_id"),
            targetIp = rs.getString("target_ip")
        )
    }
}
