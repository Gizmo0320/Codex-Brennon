package com.envarcade.brennon.database.repository.sql

import com.envarcade.brennon.common.config.DataSharingMode
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import com.envarcade.brennon.database.repository.StatsRepository
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * SQL implementation of the StatsRepository.
 * Network-aware: uses `__global__` sentinel for GLOBAL mode,
 * or the actual networkId for NETWORK mode.
 */
class SQLStatsRepository(
    private val driver: SQLDatabaseDriver,
    private val networkContext: NetworkContext
) : StatsRepository {

    /** The effective network_id value used in all queries */
    private val effectiveNetworkId: String =
        if (networkContext.sharing.stats == DataSharingMode.GLOBAL) "__global__"
        else networkContext.networkId

    private val upsertSQL: String = if (driver.isPostgres) {
        """
        INSERT INTO brennon_stats (player_uuid, stat_id, network_id, value)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (player_uuid, stat_id, network_id) DO UPDATE SET value = EXCLUDED.value
        """.trimIndent()
    } else {
        """
        INSERT INTO brennon_stats (player_uuid, stat_id, network_id, value)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE value = VALUES(value)
        """.trimIndent()
    }

    private val incrementSQL: String = if (driver.isPostgres) {
        """
        INSERT INTO brennon_stats (player_uuid, stat_id, network_id, value)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (player_uuid, stat_id, network_id) DO UPDATE SET value = brennon_stats.value + EXCLUDED.value
        """.trimIndent()
    } else {
        """
        INSERT INTO brennon_stats (player_uuid, stat_id, network_id, value)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE value = value + VALUES(value)
        """.trimIndent()
    }

    override fun getStat(uuid: UUID, statId: String): CompletableFuture<Double> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT value FROM brennon_stats WHERE player_uuid = ? AND stat_id = ? AND network_id = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, statId)
                    stmt.setString(3, effectiveNetworkId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getDouble("value") else 0.0
                }
            }
        }
    }

    override fun getAllStats(uuid: UUID): CompletableFuture<Map<String, Double>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT stat_id, value FROM brennon_stats WHERE player_uuid = ? AND network_id = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, effectiveNetworkId)
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

    override fun setStat(uuid: UUID, statId: String, value: Double): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement(upsertSQL).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, statId)
                    stmt.setString(3, effectiveNetworkId)
                    stmt.setDouble(4, value)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun incrementStat(uuid: UUID, statId: String, amount: Double): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement(incrementSQL).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, statId)
                    stmt.setString(3, effectiveNetworkId)
                    stmt.setDouble(4, amount)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun getLeaderboard(statId: String, limit: Int): CompletableFuture<Map<UUID, Double>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT player_uuid, value FROM brennon_stats WHERE stat_id = ? AND network_id = ? ORDER BY value DESC LIMIT ?").use { stmt ->
                    stmt.setString(1, statId)
                    stmt.setString(2, effectiveNetworkId)
                    stmt.setInt(3, limit)
                    val rs = stmt.executeQuery()
                    val results = linkedMapOf<UUID, Double>()
                    while (rs.next()) {
                        results[UUID.fromString(rs.getString("player_uuid"))] = rs.getDouble("value")
                    }
                    results
                }
            }
        }
    }

    override fun getLeaderboardPosition(uuid: UUID, statId: String): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                // Get player value first
                val playerValue = conn.prepareStatement("SELECT value FROM brennon_stats WHERE player_uuid = ? AND stat_id = ? AND network_id = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, statId)
                    stmt.setString(3, effectiveNetworkId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getDouble("value") else return@supplyAsync -1
                }
                // Count players with higher value
                conn.prepareStatement("SELECT COUNT(*) AS pos FROM brennon_stats WHERE stat_id = ? AND network_id = ? AND value > ?").use { stmt ->
                    stmt.setString(1, statId)
                    stmt.setString(2, effectiveNetworkId)
                    stmt.setDouble(3, playerValue)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt("pos") + 1 else -1
                }
            }
        }
    }

    override fun resetStat(uuid: UUID, statId: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM brennon_stats WHERE player_uuid = ? AND stat_id = ? AND network_id = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, statId)
                    stmt.setString(3, effectiveNetworkId)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun resetAllStats(uuid: UUID): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM brennon_stats WHERE player_uuid = ? AND network_id = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, effectiveNetworkId)
                    stmt.executeUpdate()
                }
            }
        }
    }
}
