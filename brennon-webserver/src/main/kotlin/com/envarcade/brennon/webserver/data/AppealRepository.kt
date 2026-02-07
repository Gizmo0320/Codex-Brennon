package com.envarcade.brennon.webserver.data

import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.database.driver.MongoDatabaseDriver
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import org.bson.Document
import java.util.UUID

class AppealRepository(private val databaseManager: DatabaseManager) {

    fun initialize() {
        val driver = databaseManager.driver
        when (driver) {
            is MongoDatabaseDriver -> {
                // MongoDB creates collections on first insert
            }
            is SQLDatabaseDriver -> {
                driver.getConnection().use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS brennon_appeals (
                                id VARCHAR(36) PRIMARY KEY,
                                punishment_id VARCHAR(64) NOT NULL,
                                player_uuid VARCHAR(36) NOT NULL,
                                player_name VARCHAR(64) NOT NULL,
                                reason TEXT NOT NULL,
                                status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                                staff_response TEXT,
                                staff_uuid VARCHAR(36),
                                created_at BIGINT NOT NULL,
                                resolved_at BIGINT
                            )
                        """.trimIndent())
                    }
                    val indexes = listOf(
                        "CREATE INDEX IF NOT EXISTS idx_appeals_player ON brennon_appeals (player_uuid)",
                        "CREATE INDEX IF NOT EXISTS idx_appeals_status ON brennon_appeals (status)",
                        "CREATE INDEX IF NOT EXISTS idx_appeals_punishment ON brennon_appeals (punishment_id)"
                    )
                    conn.createStatement().use { stmt ->
                        for (sql in indexes) {
                            try { stmt.executeUpdate(sql) } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
        println("[Brennon] Appeals repository initialized.")
    }

    fun create(appeal: AppealData) {
        val driver = databaseManager.driver
        when (driver) {
            is MongoDatabaseDriver -> {
                val collection = driver.getDatabase().getCollection("appeals")
                collection.insertOne(Document()
                    .append("_id", appeal.id)
                    .append("punishmentId", appeal.punishmentId)
                    .append("playerUuid", appeal.playerUuid)
                    .append("playerName", appeal.playerName)
                    .append("reason", appeal.reason)
                    .append("status", appeal.status.name)
                    .append("staffResponse", appeal.staffResponse)
                    .append("staffUuid", appeal.staffUuid)
                    .append("createdAt", appeal.createdAt)
                    .append("resolvedAt", appeal.resolvedAt)
                )
            }
            is SQLDatabaseDriver -> {
                driver.getConnection().use { conn ->
                    conn.prepareStatement("""
                        INSERT INTO brennon_appeals
                        (id, punishment_id, player_uuid, player_name, reason, status, staff_response, staff_uuid, created_at, resolved_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { stmt ->
                        stmt.setString(1, appeal.id)
                        stmt.setString(2, appeal.punishmentId)
                        stmt.setString(3, appeal.playerUuid)
                        stmt.setString(4, appeal.playerName)
                        stmt.setString(5, appeal.reason)
                        stmt.setString(6, appeal.status.name)
                        stmt.setString(7, appeal.staffResponse)
                        stmt.setString(8, appeal.staffUuid)
                        stmt.setLong(9, appeal.createdAt)
                        stmt.setObject(10, appeal.resolvedAt)
                        stmt.executeUpdate()
                    }
                }
            }
        }
    }

    fun getById(id: String): AppealData? {
        val driver = databaseManager.driver
        return when (driver) {
            is MongoDatabaseDriver -> {
                val doc = driver.getDatabase().getCollection("appeals")
                    .find(Document("_id", id)).first() ?: return null
                docToAppeal(doc)
            }
            is SQLDatabaseDriver -> {
                driver.getConnection().use { conn ->
                    conn.prepareStatement("SELECT * FROM brennon_appeals WHERE id = ?").use { stmt ->
                        stmt.setString(1, id)
                        val rs = stmt.executeQuery()
                        if (rs.next()) rsToAppeal(rs) else null
                    }
                }
            }
            else -> null
        }
    }

    fun getByPlayer(playerUuid: String, limit: Int = 50): List<AppealData> {
        val driver = databaseManager.driver
        return when (driver) {
            is MongoDatabaseDriver -> {
                driver.getDatabase().getCollection("appeals")
                    .find(Document("playerUuid", playerUuid))
                    .sort(Document("createdAt", -1))
                    .limit(limit)
                    .map { docToAppeal(it) }
                    .toList()
            }
            is SQLDatabaseDriver -> {
                val results = mutableListOf<AppealData>()
                driver.getConnection().use { conn ->
                    conn.prepareStatement(
                        "SELECT * FROM brennon_appeals WHERE player_uuid = ? ORDER BY created_at DESC LIMIT ?"
                    ).use { stmt ->
                        stmt.setString(1, playerUuid)
                        stmt.setInt(2, limit)
                        val rs = stmt.executeQuery()
                        while (rs.next()) results.add(rsToAppeal(rs))
                    }
                }
                results
            }
            else -> emptyList()
        }
    }

    fun getPending(limit: Int, offset: Int): List<AppealData> {
        val driver = databaseManager.driver
        return when (driver) {
            is MongoDatabaseDriver -> {
                driver.getDatabase().getCollection("appeals")
                    .find(Document("status", "PENDING"))
                    .sort(Document("createdAt", -1))
                    .skip(offset)
                    .limit(limit)
                    .map { docToAppeal(it) }
                    .toList()
            }
            is SQLDatabaseDriver -> {
                val results = mutableListOf<AppealData>()
                driver.getConnection().use { conn ->
                    conn.prepareStatement(
                        "SELECT * FROM brennon_appeals WHERE status = 'PENDING' ORDER BY created_at DESC LIMIT ? OFFSET ?"
                    ).use { stmt ->
                        stmt.setInt(1, limit)
                        stmt.setInt(2, offset)
                        val rs = stmt.executeQuery()
                        while (rs.next()) results.add(rsToAppeal(rs))
                    }
                }
                results
            }
            else -> emptyList()
        }
    }

    fun hasPendingAppeal(punishmentId: String): Boolean {
        val driver = databaseManager.driver
        return when (driver) {
            is MongoDatabaseDriver -> {
                driver.getDatabase().getCollection("appeals")
                    .countDocuments(Document("punishmentId", punishmentId).append("status", "PENDING")) > 0
            }
            is SQLDatabaseDriver -> {
                driver.getConnection().use { conn ->
                    conn.prepareStatement(
                        "SELECT COUNT(*) FROM brennon_appeals WHERE punishment_id = ? AND status = 'PENDING'"
                    ).use { stmt ->
                        stmt.setString(1, punishmentId)
                        val rs = stmt.executeQuery()
                        rs.next() && rs.getInt(1) > 0
                    }
                }
            }
            else -> false
        }
    }

    fun resolve(id: String, status: AppealStatus, staffUuid: String, staffResponse: String) {
        val driver = databaseManager.driver
        val now = System.currentTimeMillis()
        when (driver) {
            is MongoDatabaseDriver -> {
                driver.getDatabase().getCollection("appeals")
                    .updateOne(
                        Document("_id", id),
                        Document("\$set", Document()
                            .append("status", status.name)
                            .append("staffUuid", staffUuid)
                            .append("staffResponse", staffResponse)
                            .append("resolvedAt", now)
                        )
                    )
            }
            is SQLDatabaseDriver -> {
                driver.getConnection().use { conn ->
                    conn.prepareStatement(
                        "UPDATE brennon_appeals SET status = ?, staff_uuid = ?, staff_response = ?, resolved_at = ? WHERE id = ?"
                    ).use { stmt ->
                        stmt.setString(1, status.name)
                        stmt.setString(2, staffUuid)
                        stmt.setString(3, staffResponse)
                        stmt.setLong(4, now)
                        stmt.setString(5, id)
                        stmt.executeUpdate()
                    }
                }
            }
        }
    }

    private fun docToAppeal(doc: Document): AppealData {
        return AppealData(
            id = doc.getString("_id"),
            punishmentId = doc.getString("punishmentId"),
            playerUuid = doc.getString("playerUuid"),
            playerName = doc.getString("playerName"),
            reason = doc.getString("reason"),
            status = try { AppealStatus.valueOf(doc.getString("status")) } catch (_: Exception) { AppealStatus.PENDING },
            staffResponse = doc.getString("staffResponse"),
            staffUuid = doc.getString("staffUuid"),
            createdAt = doc.getLong("createdAt"),
            resolvedAt = doc.getLong("resolvedAt")
        )
    }

    private fun rsToAppeal(rs: java.sql.ResultSet): AppealData {
        return AppealData(
            id = rs.getString("id"),
            punishmentId = rs.getString("punishment_id"),
            playerUuid = rs.getString("player_uuid"),
            playerName = rs.getString("player_name"),
            reason = rs.getString("reason"),
            status = try { AppealStatus.valueOf(rs.getString("status")) } catch (_: Exception) { AppealStatus.PENDING },
            staffResponse = rs.getString("staff_response"),
            staffUuid = rs.getString("staff_uuid"),
            createdAt = rs.getLong("created_at"),
            resolvedAt = rs.getLong("resolved_at").let { if (rs.wasNull()) null else it }
        )
    }
}
