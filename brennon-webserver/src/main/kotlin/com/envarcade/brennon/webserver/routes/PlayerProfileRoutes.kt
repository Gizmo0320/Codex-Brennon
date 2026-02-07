package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.webserver.WebServerConfig
import com.envarcade.brennon.webserver.auth.JwtAuth
import com.envarcade.brennon.webserver.auth.RoleMiddleware
import io.javalin.Javalin

class PlayerProfileRoutes(
    private val brennon: Brennon,
    private val jwtAuth: JwtAuth,
    private val config: WebServerConfig
) {

    fun register(app: Javalin) {

        // Player: get own profile
        app.get("/api/player/profile") { ctx ->
            if (!RoleMiddleware.requirePlayer(ctx, jwtAuth, config.apiKey)) return@get
            val uuid = RoleMiddleware.getAuthPlayerUuid(ctx, jwtAuth)
            if (uuid == null) {
                ctx.status(400).json(mapOf("error" to "Player UUID not found in token"))
                return@get
            }

            val player = brennon.corePlayerManager.getPlayer(uuid).join()
            if (player.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Player data not found"))
                return@get
            }

            val p = player.get()
            val result = mutableMapOf<String, Any?>(
                "uuid" to p.uniqueId.toString(),
                "name" to p.name,
                "ranks" to p.getRanks().map { mapOf("id" to it.id, "displayName" to it.displayName) },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "online" to p.isOnline,
                "currentServer" to p.currentServer
            )

            // Include economy balance
            try {
                val balance = brennon.coreEconomyManager.getBalance(uuid).join()
                result["balance"] = balance
            } catch (_: Exception) { }

            // Include stats if module is enabled
            if (brennon.config.modules.stats) {
                try {
                    val stats = brennon.coreStatsManager.getAllStats(uuid).join()
                    result["stats"] = stats
                } catch (_: Exception) { }
            }

            ctx.json(result)
        }

        // Player: get own active punishments (limited info, no staff details)
        app.get("/api/player/punishments") { ctx ->
            if (!RoleMiddleware.requirePlayer(ctx, jwtAuth, config.apiKey)) return@get
            val uuid = RoleMiddleware.getAuthPlayerUuid(ctx, jwtAuth)
            if (uuid == null) {
                ctx.status(400).json(mapOf("error" to "Player UUID not found in token"))
                return@get
            }

            val punishments = brennon.corePunishmentManager.getActivePunishments(uuid).join()
            ctx.json(punishments.map { p ->
                mapOf(
                    "id" to p.id,
                    "type" to p.type.name,
                    "reason" to p.reason,
                    "issuedAt" to p.issuedAt.toString(),
                    "expiresAt" to p.expiresAt?.toString(),
                    "isPermanent" to p.isPermanent
                )
            })
        }
    }
}
