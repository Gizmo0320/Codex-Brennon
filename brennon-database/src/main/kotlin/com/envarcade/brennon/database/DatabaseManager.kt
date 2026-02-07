package com.envarcade.brennon.database

import com.envarcade.brennon.common.config.DatabaseConfig
import com.envarcade.brennon.common.config.DatabaseDriver
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.database.driver.BrennonDatabaseDriver
import com.envarcade.brennon.database.driver.MongoDatabaseDriver
import com.envarcade.brennon.database.driver.SQLDatabaseDriver
import com.envarcade.brennon.database.repository.PlayerRepository
import com.envarcade.brennon.database.repository.PunishmentRepository
import com.envarcade.brennon.database.repository.RankRepository
import com.envarcade.brennon.database.repository.StatsRepository
import com.envarcade.brennon.database.repository.TicketRepository

class DatabaseManager(
    private val config: DatabaseConfig,
    private val networkContext: NetworkContext
) {

    lateinit var driver: BrennonDatabaseDriver
        private set

    lateinit var players: PlayerRepository
        private set

    lateinit var ranks: RankRepository
        private set

    lateinit var punishments: PunishmentRepository
        private set

    lateinit var tickets: TicketRepository
        private set

    lateinit var stats: StatsRepository
        private set

    fun initialize() {
        println("[Brennon] Initializing database driver: ${config.driver}")

        driver = when (config.driver) {
            DatabaseDriver.MONGODB -> MongoDatabaseDriver(config)
            DatabaseDriver.MYSQL,
            DatabaseDriver.MARIADB,
            DatabaseDriver.POSTGRESQL -> SQLDatabaseDriver(config, networkContext)
        }

        driver.connect()

        players = driver.createPlayerRepository()
        ranks = driver.createRankRepository()
        punishments = driver.createPunishmentRepository(networkContext)
        tickets = driver.createTicketRepository(networkContext)
        stats = driver.createStatsRepository(networkContext)

        println("[Brennon] Database initialized successfully. Network: ${networkContext.networkId}")
    }

    fun shutdown() {
        println("[Brennon] Shutting down database...")
        driver.disconnect()
    }

    fun isConnected(): Boolean = driver.isConnected()
}
