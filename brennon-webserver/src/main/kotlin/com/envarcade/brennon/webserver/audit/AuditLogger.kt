package com.envarcade.brennon.webserver.audit

import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.database.driver.MongoDatabaseDriver
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import com.envarcade.brennon.webserver.AuditConfig
import com.google.gson.Gson
import org.bson.Document
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class AuditLogger(
    private val config: AuditConfig,
    private val databaseManager: DatabaseManager,
    private val dataFolder: File
) {
    private val gson = Gson()
    private val auditFile = File(dataFolder, "audit.log")
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Brennon-Audit").apply { isDaemon = true }
    }

    fun initialize() {
        if (!config.enabled) return

        if (config.logToDatabase) {
            try {
                createDatabaseSchema()
                println("[Brennon] Audit log database schema initialized.")
            } catch (e: Exception) {
                println("[Brennon] Failed to initialize audit log database: ${e.message}")
            }
        }

        if (config.logToFile) {
            auditFile.parentFile?.mkdirs()
            println("[Brennon] Audit log file: ${auditFile.absolutePath}")
        }

        println("[Brennon] Audit logging enabled (file=${config.logToFile}, database=${config.logToDatabase})")
    }

    fun log(entry: AuditLogEntry) {
        if (!config.enabled) return

        executor.submit {
            try {
                if (config.logToFile) writeToFile(entry)
                if (config.logToDatabase) writeToDatabase(entry)
            } catch (e: Exception) {
                println("[Brennon] Audit log error: ${e.message}")
            }
        }
    }

    fun shutdown() {
        executor.shutdown()
    }

    // Query methods for AuditRoutes

    fun getRecentEntries(limit: Int, offset: Int): List<AuditLogEntry> {
        val driver = databaseManager.driver
        return when (driver) {
            is MongoDatabaseDriver -> queryMongo(limit, offset)
            is SQLDatabaseDriver -> querySQL(driver, limit, offset)
            else -> emptyList()
        }
    }

    fun getEntriesByUser(username: String, limit: Int): List<AuditLogEntry> {
        val driver = databaseManager.driver
        return when (driver) {
            is MongoDatabaseDriver -> queryMongoByField("username", username, limit)
            is SQLDatabaseDriver -> querySQLByField(driver, "username", username, limit)
            else -> emptyList()
        }
    }

    fun getEntriesByCategory(category: AuditCategory, limit: Int): List<AuditLogEntry> {
        val driver = databaseManager.driver
        return when (driver) {
            is MongoDatabaseDriver -> queryMongoByField("category", category.name, limit)
            is SQLDatabaseDriver -> querySQLByField(driver, "category", category.name, limit)
            else -> emptyList()
        }
    }

    // File I/O

    private fun writeToFile(entry: AuditLogEntry) {
        PrintWriter(FileWriter(auditFile, true)).use { writer ->
            writer.println(gson.toJson(entry))
        }
    }

    // Database I/O

    private fun createDatabaseSchema() {
        val driver = databaseManager.driver
        when (driver) {
            is MongoDatabaseDriver -> {
                // MongoDB creates collections on first insert, nothing to do
            }
            is SQLDatabaseDriver -> {
                driver.getConnection().use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS brennon_audit_log (
                                id VARCHAR(36) PRIMARY KEY,
                                timestamp BIGINT NOT NULL,
                                username VARCHAR(64) NOT NULL,
                                action VARCHAR(8) NOT NULL,
                                path VARCHAR(256) NOT NULL,
                                status_code INT NOT NULL,
                                target_info TEXT,
                                request_body TEXT,
                                ip_address VARCHAR(45),
                                category VARCHAR(32) NOT NULL
                            )
                        """.trimIndent())
                    }
                    // Create indexes for common queries
                    val indexStatements = listOf(
                        "CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON brennon_audit_log (timestamp DESC)",
                        "CREATE INDEX IF NOT EXISTS idx_audit_username ON brennon_audit_log (username)",
                        "CREATE INDEX IF NOT EXISTS idx_audit_category ON brennon_audit_log (category)"
                    )
                    conn.createStatement().use { stmt ->
                        for (sql in indexStatements) {
                            try { stmt.executeUpdate(sql) } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
    }

    private fun writeToDatabase(entry: AuditLogEntry) {
        val driver = databaseManager.driver
        when (driver) {
            is MongoDatabaseDriver -> {
                val collection = driver.getDatabase().getCollection("audit_log")
                val doc = Document()
                    .append("_id", entry.id)
                    .append("timestamp", entry.timestamp)
                    .append("username", entry.username)
                    .append("action", entry.action)
                    .append("path", entry.path)
                    .append("statusCode", entry.statusCode)
                    .append("targetInfo", entry.targetInfo)
                    .append("requestBody", entry.requestBody)
                    .append("ipAddress", entry.ipAddress)
                    .append("category", entry.category.name)
                collection.insertOne(doc)
            }
            is SQLDatabaseDriver -> {
                driver.getConnection().use { conn ->
                    conn.prepareStatement("""
                        INSERT INTO brennon_audit_log
                        (id, timestamp, username, action, path, status_code, target_info, request_body, ip_address, category)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { stmt ->
                        stmt.setString(1, entry.id)
                        stmt.setLong(2, entry.timestamp)
                        stmt.setString(3, entry.username)
                        stmt.setString(4, entry.action)
                        stmt.setString(5, entry.path)
                        stmt.setInt(6, entry.statusCode)
                        stmt.setString(7, entry.targetInfo)
                        stmt.setString(8, entry.requestBody)
                        stmt.setString(9, entry.ipAddress)
                        stmt.setString(10, entry.category.name)
                        stmt.executeUpdate()
                    }
                }
            }
        }
    }

    // Query helpers

    private fun queryMongo(limit: Int, offset: Int): List<AuditLogEntry> {
        val driver = databaseManager.driver as MongoDatabaseDriver
        val collection = driver.getDatabase().getCollection("audit_log")
        return collection.find()
            .sort(Document("timestamp", -1))
            .skip(offset)
            .limit(limit)
            .map { docToEntry(it) }
            .toList()
    }

    private fun queryMongoByField(field: String, value: String, limit: Int): List<AuditLogEntry> {
        val driver = databaseManager.driver as MongoDatabaseDriver
        val collection = driver.getDatabase().getCollection("audit_log")
        return collection.find(Document(field, value))
            .sort(Document("timestamp", -1))
            .limit(limit)
            .map { docToEntry(it) }
            .toList()
    }

    private fun docToEntry(doc: Document): AuditLogEntry {
        return AuditLogEntry(
            id = doc.getString("_id"),
            timestamp = doc.getLong("timestamp"),
            username = doc.getString("username"),
            action = doc.getString("action"),
            path = doc.getString("path"),
            statusCode = doc.getInteger("statusCode"),
            targetInfo = doc.getString("targetInfo"),
            requestBody = doc.getString("requestBody"),
            ipAddress = doc.getString("ipAddress"),
            category = try { AuditCategory.valueOf(doc.getString("category")) } catch (_: Exception) { AuditCategory.OTHER }
        )
    }

    private fun querySQL(driver: SQLDatabaseDriver, limit: Int, offset: Int): List<AuditLogEntry> {
        val entries = mutableListOf<AuditLogEntry>()
        driver.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT * FROM brennon_audit_log ORDER BY timestamp DESC LIMIT ? OFFSET ?"
            ).use { stmt ->
                stmt.setInt(1, limit)
                stmt.setInt(2, offset)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    entries.add(rsToEntry(rs))
                }
            }
        }
        return entries
    }

    private fun querySQLByField(driver: SQLDatabaseDriver, field: String, value: String, limit: Int): List<AuditLogEntry> {
        val entries = mutableListOf<AuditLogEntry>()
        driver.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT * FROM brennon_audit_log WHERE $field = ? ORDER BY timestamp DESC LIMIT ?"
            ).use { stmt ->
                stmt.setString(1, value)
                stmt.setInt(2, limit)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    entries.add(rsToEntry(rs))
                }
            }
        }
        return entries
    }

    private fun rsToEntry(rs: java.sql.ResultSet): AuditLogEntry {
        return AuditLogEntry(
            id = rs.getString("id"),
            timestamp = rs.getLong("timestamp"),
            username = rs.getString("username"),
            action = rs.getString("action"),
            path = rs.getString("path"),
            statusCode = rs.getInt("status_code"),
            targetInfo = rs.getString("target_info"),
            requestBody = rs.getString("request_body"),
            ipAddress = rs.getString("ip_address"),
            category = try { AuditCategory.valueOf(rs.getString("category")) } catch (_: Exception) { AuditCategory.OTHER }
        )
    }
}
