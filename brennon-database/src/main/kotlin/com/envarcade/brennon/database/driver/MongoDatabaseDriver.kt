package com.envarcade.brennon.database.driver

import com.envarcade.brennon.common.config.DatabaseConfig
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.database.repository.PlayerRepository
import com.envarcade.brennon.database.repository.PunishmentRepository
import com.envarcade.brennon.database.repository.RankRepository
import com.envarcade.brennon.database.repository.StatsRepository
import com.envarcade.brennon.database.repository.TicketRepository
import com.envarcade.brennon.database.repository.mongo.MongoPlayerRepository
import com.envarcade.brennon.database.repository.mongo.MongoPunishmentRepository
import com.envarcade.brennon.database.repository.mongo.MongoRankRepository
import com.envarcade.brennon.database.repository.mongo.MongoStatsRepository
import com.envarcade.brennon.database.repository.mongo.MongoTicketRepository
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase

class MongoDatabaseDriver(private val config: DatabaseConfig) : BrennonDatabaseDriver {

    private var client: MongoClient? = null
    private var database: MongoDatabase? = null

    override fun connect() {
        val connectionString = if (config.uri.isNotBlank()) {
            config.uri
        } else {
            buildString {
                append("mongodb://")
                if (config.username.isNotBlank()) {
                    append("${config.username}:${config.password}@")
                }
                append("${config.host}:${config.port}/${config.database}")
            }
        }

        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(connectionString))
            .build()

        client = MongoClients.create(settings)
        database = client!!.getDatabase(config.database)

        database!!.listCollectionNames().first()
        println("[Brennon] Connected to MongoDB: ${config.database}")
    }

    override fun disconnect() {
        client?.close()
        client = null
        database = null
    }

    override fun isConnected(): Boolean = client != null

    fun getDatabase(): MongoDatabase =
        database ?: throw IllegalStateException("MongoDB is not connected!")

    override fun createPlayerRepository(): PlayerRepository = MongoPlayerRepository(getDatabase())
    override fun createRankRepository(): RankRepository = MongoRankRepository(getDatabase())
    override fun createPunishmentRepository(networkContext: NetworkContext): PunishmentRepository = MongoPunishmentRepository(getDatabase(), networkContext)
    override fun createTicketRepository(networkContext: NetworkContext): TicketRepository = MongoTicketRepository(getDatabase(), networkContext)
    override fun createStatsRepository(networkContext: NetworkContext): StatsRepository = MongoStatsRepository(getDatabase(), networkContext)
}
