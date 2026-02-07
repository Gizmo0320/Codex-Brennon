package com.envarcade.brennon.database.repository.sql

import com.envarcade.brennon.common.model.RankData
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import com.envarcade.brennon.database.repository.RankRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.CompletableFuture

/**
 * SQL implementation of the RankRepository.
 * Supports MySQL, MariaDB, and PostgreSQL.
 */
class SQLRankRepository(private val driver: SQLDatabaseDriver) : RankRepository {

    private val gson = Gson()

    private val upsertSQL: String = if (driver.isPostgres) {
        """
        INSERT INTO brennon_ranks (id, display_name, prefix, suffix, weight, permissions, inheritance, is_default, is_staff, metadata)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            display_name = EXCLUDED.display_name,
            prefix = EXCLUDED.prefix,
            suffix = EXCLUDED.suffix,
            weight = EXCLUDED.weight,
            permissions = EXCLUDED.permissions,
            inheritance = EXCLUDED.inheritance,
            is_default = EXCLUDED.is_default,
            is_staff = EXCLUDED.is_staff,
            metadata = EXCLUDED.metadata
        """.trimIndent()
    } else {
        """
        INSERT INTO brennon_ranks (id, display_name, prefix, suffix, weight, permissions, inheritance, is_default, is_staff, metadata)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            display_name = VALUES(display_name),
            prefix = VALUES(prefix),
            suffix = VALUES(suffix),
            weight = VALUES(weight),
            permissions = VALUES(permissions),
            inheritance = VALUES(inheritance),
            is_default = VALUES(is_default),
            is_staff = VALUES(is_staff),
            metadata = VALUES(metadata)
        """.trimIndent()
    }

    override fun findById(id: String): CompletableFuture<RankData?> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_ranks WHERE id = ?").use { stmt ->
                    stmt.setString(1, id)
                    val rs = stmt.executeQuery()
                    if (rs.next()) fromResultSet(rs) else null
                }
            }
        }
    }

    override fun findAll(): CompletableFuture<List<RankData>> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_ranks ORDER BY weight DESC").use { stmt ->
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<RankData>()
                    while (rs.next()) list.add(fromResultSet(rs))
                    list
                }
            }
        }
    }

    override fun findDefault(): CompletableFuture<RankData?> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM brennon_ranks WHERE is_default = TRUE LIMIT 1").use { stmt ->
                    val rs = stmt.executeQuery()
                    if (rs.next()) fromResultSet(rs) else null
                }
            }
        }
    }

    override fun save(rank: RankData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement(upsertSQL).use { stmt ->
                    stmt.setString(1, rank.id)
                    stmt.setString(2, rank.displayName)
                    stmt.setString(3, rank.prefix)
                    stmt.setString(4, rank.suffix)
                    stmt.setInt(5, rank.weight)
                    stmt.setString(6, gson.toJson(rank.permissions))
                    stmt.setString(7, gson.toJson(rank.inheritance))
                    stmt.setBoolean(8, rank.isDefault)
                    stmt.setBoolean(9, rank.isStaff)
                    stmt.setString(10, gson.toJson(rank.metadata))
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun delete(id: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM brennon_ranks WHERE id = ?").use { stmt ->
                    stmt.setString(1, id)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun exists(id: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            driver.getConnection().use { conn ->
                conn.prepareStatement("SELECT 1 FROM brennon_ranks WHERE id = ?").use { stmt ->
                    stmt.setString(1, id)
                    stmt.executeQuery().next()
                }
            }
        }
    }

    private fun fromResultSet(rs: java.sql.ResultSet): RankData {
        val setType = object : TypeToken<MutableSet<String>>() {}.type
        val mapType = object : TypeToken<MutableMap<String, String>>() {}.type

        return RankData(
            id = rs.getString("id"),
            displayName = rs.getString("display_name") ?: rs.getString("id"),
            prefix = rs.getString("prefix") ?: "",
            suffix = rs.getString("suffix") ?: "",
            weight = rs.getInt("weight"),
            permissions = gson.fromJson(rs.getString("permissions") ?: "[]", setType),
            inheritance = gson.fromJson(rs.getString("inheritance") ?: "[]", setType),
            isDefault = rs.getBoolean("is_default"),
            isStaff = rs.getBoolean("is_staff"),
            metadata = gson.fromJson(rs.getString("metadata") ?: "{}", mapType)
        )
    }
}
