package com.envarcade.brennon.database.migration

import com.envarcade.brennon.common.config.DataSharingMode
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.database.driver.SQLDatabaseDriver

/**
 * Handles schema migrations for multi-network support.
 *
 * Adds `network_id` columns to existing tables and backfills
 * existing rows with the current network's ID. Idempotent — safe
 * to run on every startup.
 */
class SchemaMigrator(
    private val driver: SQLDatabaseDriver,
    private val networkContext: NetworkContext
) {

    fun migrate() {
        migrateTable("brennon_punishments", networkContext.networkId)
        migrateTable("brennon_tickets", networkContext.networkId)
        migrateStatsTable()
    }

    private fun migrateTable(table: String, backfillValue: String) {
        driver.getConnection().use { conn ->
            val hasColumn = conn.metaData.getColumns(null, null, table, "network_id").use { it.next() }
            if (hasColumn) {
                // Column exists — backfill any NULL rows from before migration
                conn.prepareStatement("UPDATE $table SET network_id = ? WHERE network_id IS NULL").use { stmt ->
                    stmt.setString(1, backfillValue)
                    val updated = stmt.executeUpdate()
                    if (updated > 0) {
                        println("[Brennon] Migrated $updated rows in $table with network_id=$backfillValue")
                    }
                }
            }
            // If column doesn't exist, createTables() already created it with the new schema.
            // This handles the case where the table was created by an older version without network_id.
        }
    }

    private fun migrateStatsTable() {
        driver.getConnection().use { conn ->
            val hasColumn = conn.metaData.getColumns(null, null, "brennon_stats", "network_id").use { it.next() }
            if (!hasColumn) {
                // Old schema without network_id — this means createTables() didn't create it fresh
                // (table already existed from a prior version). We need to add the column and rebuild PK.
                try {
                    conn.autoCommit = false

                    // Add column
                    conn.createStatement().use { stmt ->
                        stmt.execute("ALTER TABLE brennon_stats ADD COLUMN network_id VARCHAR(32) NOT NULL DEFAULT '__global__'")
                    }

                    // Backfill with appropriate value
                    val backfillValue = if (networkContext.sharing.stats == DataSharingMode.GLOBAL) {
                        "__global__"
                    } else {
                        networkContext.networkId
                    }
                    conn.prepareStatement("UPDATE brennon_stats SET network_id = ? WHERE network_id = '__global__'").use { stmt ->
                        stmt.setString(1, backfillValue)
                        stmt.executeUpdate()
                    }

                    // Rebuild PK to include network_id
                    if (driver.isPostgres) {
                        conn.createStatement().use { stmt ->
                            stmt.execute("ALTER TABLE brennon_stats DROP CONSTRAINT brennon_stats_pkey")
                            stmt.execute("ALTER TABLE brennon_stats ADD PRIMARY KEY (player_uuid, stat_id, network_id)")
                        }
                    } else {
                        conn.createStatement().use { stmt ->
                            stmt.execute("ALTER TABLE brennon_stats DROP PRIMARY KEY, ADD PRIMARY KEY (player_uuid, stat_id, network_id)")
                        }
                    }

                    conn.commit()
                    println("[Brennon] Migrated brennon_stats table to include network_id column")
                } catch (e: Exception) {
                    conn.rollback()
                    println("[Brennon] Stats migration failed (may already be migrated): ${e.message}")
                } finally {
                    conn.autoCommit = true
                }
            } else {
                // Column exists — backfill any default values if sharing mode changed
                val backfillValue = if (networkContext.sharing.stats == DataSharingMode.GLOBAL) {
                    "__global__"
                } else {
                    networkContext.networkId
                }
                // Only backfill rows that still have the default __global__ value when mode is NETWORK
                if (networkContext.sharing.stats == DataSharingMode.NETWORK) {
                    conn.prepareStatement(
                        "UPDATE brennon_stats SET network_id = ? WHERE network_id = '__global__'"
                    ).use { stmt ->
                        stmt.setString(1, backfillValue)
                        val updated = stmt.executeUpdate()
                        if (updated > 0) {
                            println("[Brennon] Migrated $updated stats rows from __global__ to $backfillValue")
                        }
                    }
                }
            }
        }
    }
}
