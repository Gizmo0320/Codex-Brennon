package com.envarcade.brennon.database.driver

import com.envarcade.brennon.common.config.DatabaseConfig
import com.envarcade.brennon.common.config.DatabaseDriver
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.database.migration.SchemaMigrator
import com.envarcade.brennon.database.repository.PlayerRepository
import com.envarcade.brennon.database.repository.PunishmentRepository
import com.envarcade.brennon.database.repository.RankRepository
import com.envarcade.brennon.database.repository.StatsRepository
import com.envarcade.brennon.database.repository.TicketRepository
import com.envarcade.brennon.database.repository.sql.SQLPlayerRepository
import com.envarcade.brennon.database.repository.sql.SQLPunishmentRepository
import com.envarcade.brennon.database.repository.sql.SQLRankRepository
import com.envarcade.brennon.database.repository.sql.SQLStatsRepository
import com.envarcade.brennon.database.repository.sql.SQLTicketRepository
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

/**
 * SQL database driver implementation supporting MySQL, MariaDB, and PostgreSQL.
 * Uses HikariCP for connection pooling.
 */
class SQLDatabaseDriver(
    private val config: DatabaseConfig,
    private val networkContext: NetworkContext
) : BrennonDatabaseDriver {

    private var dataSource: HikariDataSource? = null

    /** Exposed so repositories can generate driver-appropriate SQL */
    val driverType: DatabaseDriver get() = config.driver

    /** Whether this driver is PostgreSQL (changes upsert syntax) */
    val isPostgres: Boolean get() = config.driver == DatabaseDriver.POSTGRESQL

    override fun connect() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = buildJdbcUrl()
            username = config.username
            password = config.password
            maximumPoolSize = config.poolSize
            minimumIdle = 2
            connectionTimeout = 10000
            idleTimeout = 300000
            maxLifetime = 600000
            poolName = "Brennon-SQL-Pool"

            when (config.driver) {
                DatabaseDriver.MYSQL, DatabaseDriver.MARIADB -> {
                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                    addDataSourceProperty("useServerPrepStmts", "true")
                }
                DatabaseDriver.POSTGRESQL -> {
                    addDataSourceProperty("reWriteBatchedInserts", "true")
                }
                else -> {}
            }
        }

        dataSource = HikariDataSource(hikariConfig)
        createTables()

        // Run schema migrations for network support
        SchemaMigrator(this, networkContext).migrate()

        println("[Brennon] Connected to ${config.driver}: ${config.database}")
    }

    override fun disconnect() {
        dataSource?.close()
        dataSource = null
    }

    override fun isConnected(): Boolean = dataSource?.isClosed == false

    fun getConnection(): Connection =
        dataSource?.connection ?: throw IllegalStateException("SQL database is not connected!")

    private fun buildJdbcUrl(): String = when (config.driver) {
        DatabaseDriver.MYSQL, DatabaseDriver.MARIADB ->
            "jdbc:mysql://${config.host}:${config.port}/${config.database}?useSSL=false&allowPublicKeyRetrieval=true"
        DatabaseDriver.POSTGRESQL ->
            "jdbc:postgresql://${config.host}:${config.port}/${config.database}"
        else -> throw IllegalArgumentException("Unsupported SQL driver: ${config.driver}")
    }

    private fun createTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val doubleType = if (isPostgres) "DOUBLE PRECISION" else "DOUBLE"

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS brennon_players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        primary_rank VARCHAR(64) DEFAULT 'default',
                        ranks TEXT,
                        permissions TEXT,
                        balance $doubleType DEFAULT 0.0,
                        first_join BIGINT NOT NULL,
                        last_seen BIGINT NOT NULL,
                        last_server VARCHAR(64),
                        ip_address VARCHAR(45),
                        playtime BIGINT DEFAULT 0,
                        metadata TEXT
                    )
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS brennon_ranks (
                        id VARCHAR(64) PRIMARY KEY,
                        display_name VARCHAR(128),
                        prefix TEXT,
                        suffix TEXT,
                        weight INT DEFAULT 0,
                        permissions TEXT,
                        inheritance TEXT,
                        is_default BOOLEAN DEFAULT FALSE,
                        is_staff BOOLEAN DEFAULT FALSE,
                        metadata TEXT
                    )
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS brennon_punishments (
                        id VARCHAR(36) PRIMARY KEY,
                        target VARCHAR(36) NOT NULL,
                        issuer VARCHAR(36),
                        type VARCHAR(16) NOT NULL,
                        reason TEXT NOT NULL,
                        issued_at BIGINT NOT NULL,
                        expires_at BIGINT,
                        active BOOLEAN DEFAULT TRUE,
                        revoked_by VARCHAR(36),
                        revoked_at BIGINT,
                        revoke_reason TEXT,
                        network_id VARCHAR(32)
                    )
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS brennon_tickets (
                        id VARCHAR(16) PRIMARY KEY,
                        creator VARCHAR(36) NOT NULL,
                        creator_name VARCHAR(16) NOT NULL,
                        assignee VARCHAR(36),
                        subject TEXT NOT NULL,
                        status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
                        priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
                        server VARCHAR(64),
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        closed_at BIGINT,
                        network_id VARCHAR(32)
                    )
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS brennon_ticket_messages (
                        id ${if (isPostgres) "SERIAL" else "INT AUTO_INCREMENT"} PRIMARY KEY,
                        ticket_id VARCHAR(16) NOT NULL,
                        author VARCHAR(36) NOT NULL,
                        author_name VARCHAR(16) NOT NULL,
                        content TEXT NOT NULL,
                        timestamp BIGINT NOT NULL,
                        is_staff BOOLEAN DEFAULT FALSE
                    )
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS brennon_stats (
                        player_uuid VARCHAR(36) NOT NULL,
                        stat_id VARCHAR(64) NOT NULL,
                        network_id VARCHAR(32) NOT NULL DEFAULT '__global__',
                        value $doubleType DEFAULT 0.0,
                        PRIMARY KEY (player_uuid, stat_id, network_id)
                    )
                """.trimIndent())

                // Migrations
                try {
                    stmt.execute("ALTER TABLE brennon_punishments ADD COLUMN target_ip VARCHAR(45)")
                } catch (_: Exception) { /* column already exists */ }

                // Create indexes
                try {
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_name ON brennon_players(name)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_target ON brennon_punishments(target)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_active ON brennon_punishments(target, type, active)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_network ON brennon_punishments(network_id)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_ip ON brennon_punishments(target_ip, type, active)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_tickets_creator ON brennon_tickets(creator)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_tickets_assignee ON brennon_tickets(assignee)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_tickets_status ON brennon_tickets(status)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_tickets_network ON brennon_tickets(network_id)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket ON brennon_ticket_messages(ticket_id)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_leaderboard ON brennon_stats(stat_id, value)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_network ON brennon_stats(network_id)")
                } catch (_: Exception) { }
            }
        }
    }

    override fun createPlayerRepository(): PlayerRepository = SQLPlayerRepository(this)
    override fun createRankRepository(): RankRepository = SQLRankRepository(this)
    override fun createPunishmentRepository(networkContext: NetworkContext): PunishmentRepository = SQLPunishmentRepository(this, networkContext)
    override fun createTicketRepository(networkContext: NetworkContext): TicketRepository = SQLTicketRepository(this, networkContext)
    override fun createStatsRepository(networkContext: NetworkContext): StatsRepository = SQLStatsRepository(this, networkContext)
}
