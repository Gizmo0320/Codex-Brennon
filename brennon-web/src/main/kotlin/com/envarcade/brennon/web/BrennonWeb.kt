package com.envarcade.brennon.web

import com.envarcade.brennon.common.config.WebConfig
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.web.routes.*
import spark.Spark

/**
 * Brennon Web Module — REST API.
 *
 * Provides a full CRUD REST API backed by SparkJava for managing
 * all Brennon systems: players, ranks, punishments, economy,
 * tickets, stats, and servers.
 */
class BrennonWeb(
    private val brennon: Brennon,
    private val config: WebConfig
) {

    fun initialize() {
        if (!config.enabled) {
            println("[Brennon] Web API is disabled.")
            return
        }

        println("[Brennon] Starting Web API on port ${config.port}...")

        Spark.port(config.port)

        // CORS
        Spark.before(spark.Filter { _, response ->
            response.header("Access-Control-Allow-Origin", config.corsOrigins)
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization")
            response.type("application/json")
        })

        Spark.options("/*") { _, response ->
            response.status(200)
            ""
        }

        // Auth filter
        WebAuth(config).register()

        // Register routes
        PlayerRoutes(brennon).register()
        RankRoutes(brennon).register()
        PunishmentRoutes(brennon).register()
        EconomyRoutes(brennon).register()
        ServerRoutes(brennon).register()

        if (brennon.config.modules.tickets) {
            TicketRoutes(brennon).register()
        }
        if (brennon.config.modules.stats) {
            StatsRoutes(brennon).register()
        }

        Spark.awaitInitialization()
        println("[Brennon] Web API started on port ${config.port}.")
    }

    fun shutdown() {
        Spark.stop()
        println("[Brennon] Web API stopped.")
    }
}
