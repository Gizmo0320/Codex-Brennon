package com.envarcade.brennon.database.repository.sql

import com.envarcade.brennon.common.model.PlayerData
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import com.envarcade.brennon.database.repository.PlayerRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * SQL implementation of the PlayerRepository.
 * Supports MySQL, MariaDB, and PostgreSQL with appropriate upsert syntax.
 */
class SQLPlayerRepository(private val driver: SQLDatabaseDriver) : PlayerRepository {

    private val gson = Gson()

    private val upsertSQL: String = if (driver.isPostgres) {
        """
        INSERT INTO brennon_players (uuid, name, primary_rank, ranks, permissions, balance, first_join, last_seen, last_server, ip_address, playtime, metadata)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (uuid) DO UPDATE SET
            name = EXCLUDED.name,
            primary_rank = EXCLUDED.primary_rank,
            ranks = EXCLUDED.ranks,
            permissions = EXCLUDED.permissions,
            balance = EXCLUDED.balance,
            last_seen = EXCLUDED.last_seen,
            last_server = EXCLUDED.last_server,
            ip_address = EXCLUDED.ip_address,
            playtime = EXCLUDED.playtime,
            metadata = EXCLUDED.metadata
        """.trimIndent()
    } else {
        """
        INSERT INTO brennon_players (uuid, name, primary_rank, ranks, permissions, balance, first_join, last_seen, last_server, ip_address, playtime, metadata)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            primary_rank = VALUES(primary_rank),
            ranks = VALUES(ranks),
            permissions = VALUES(permissions),
            balance = VALUES(balance),
            last_seen = VALUES(last_seen),
            last_server = VALUES(last_server),
            ip_address = VALUES(ip_address),
            playtime = VALUES(playtime),
            metadata = VALUES(metadata)
        """.trimIndent()
    }

    override fun findByUuid(uuid: UUID): CompletableFuture<PlayerData?> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_players WHERE uuid = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    if (rs.next()) fromResultSet(rs) else null
                }
            }
        }
    }

    override fun findByName(name: String): CompletableFuture<PlayerData?> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_players WHERE LOWER(name) = LOWER(?)").use { stmt ->
                    stmt.setString(1, name)
                    val rs = stmt.executeQuery()
                    if (rs.next()) fromResultSet(rs) else null
                }
            }
        }
    }

    override fun save(player: PlayerData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement(upsertSQL).use { stmt ->
                    stmt.setString(1, player.uuid.toString())
                    stmt.setString(2, player.name)
                    stmt.setString(3, player.primaryRank)
                    stmt.setString(4, gson.toJson(player.ranks))
                    stmt.setString(5, gson.toJson(player.permissions))
                    stmt.setDouble(6, player.balance)
                    stmt.setLong(7, player.firstJoin.toEpochMilli())
                    stmt.setLong(8, player.lastSeen.toEpochMilli())
                    stmt.setString(9, player.lastServer)
                    stmt.setString(10, player.ipAddress)
                    stmt.setLong(11, player.playtime)
                    stmt.setString(12, gson.toJson(player.metadata))
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun delete(uuid: UUID): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM brennon_players WHERE uuid = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun exists(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT 1 FROM brennon_players WHERE uuid = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeQuery().next()
                }
            }
        }
    }

    override fun findByIp(ip: String): CompletableFuture<List<PlayerData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_players WHERE ip_address = ?").use { stmt ->
                    stmt.setString(1, ip)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<PlayerData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list
                }
            }
        }
    }

    override fun countAll(): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT COUNT(*) FROM brennon_players").use { stmt ->
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getLong(1) else 0L
                }
            }
        }
    }

    override fun findRecent(limit: Int, offset: Int): CompletableFuture<List<PlayerData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_players ORDER BY last_seen DESC LIMIT ? OFFSET ?").use { stmt ->
                    stmt.setInt(1, limit)
                    stmt.setInt(2, offset)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<PlayerData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list
                }
            }
        }
    }

    private fun fromResultSet(rs: java.sql.ResultSet): PlayerData {
        val setType = object : TypeToken<MutableSet<String>>() {}.type
        val mapType = object : TypeToken<MutableMap<String, String>>() {}.type

        return PlayerData(
            uuid = UUID.fromString(rs.getString("uuid")),
            name = rs.getString("name"),
            primaryRank = rs.getString("primary_rank") ?: "default",
            ranks = gson.fromJson(rs.getString("ranks") ?: "[\"default\"]", setType),
            permissions = gson.fromJson(rs.getString("permissions") ?: "[]", setType),
            balance = rs.getDouble("balance"),
            firstJoin = Instant.ofEpochMilli(rs.getLong("first_join")),
            lastSeen = Instant.ofEpochMilli(rs.getLong("last_seen")),
            lastServer = rs.getString("last_server") ?: "",
            ipAddress = rs.getString("ip_address") ?: "",
            playtime = rs.getLong("playtime"),
            metadata = gson.fromJson(rs.getString("metadata") ?: "{}", mapType)
        )
    }
}
