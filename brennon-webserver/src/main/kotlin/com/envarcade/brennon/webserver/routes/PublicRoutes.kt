package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class PublicRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {

        // Public: server list with player counts
        app.get("/api/public/servers") { ctx ->
            val servers = brennon.coreServerManager.getServers().filter { it.isOnline }.map { s ->
                mapOf(
                    "name" to s.name,
                    "group" to s.group,
                    "playerCount" to s.playerCount,
                    "maxPlayers" to s.maxPlayers,
                    "online" to s.isOnline
                )
            }
            val totalPlayers = brennon.coreServerManager.getNetworkPlayerCount()
            ctx.json(mapOf(
                "totalPlayers" to totalPlayers,
                "serverCount" to servers.size,
                "servers" to servers
            ))
        }

        // Public: leaderboard for a stat
        app.get("/api/public/leaderboard/{statId}") { ctx ->
            if (!brennon.config.modules.stats) {
                ctx.status(404).json(mapOf("error" to "Stats module is disabled"))
                return@get
            }

            val statId = ctx.pathParam("statId")
            val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 10
            val leaderboard = brennon.coreStatsManager.getLeaderboard(statId, limit).join()
            val entries = leaderboard.entries.mapIndexed { index, (uuid, value) ->
                val name = brennon.corePlayerManager.getCachedPlayer(uuid)?.name ?: uuid.toString()
                mapOf("rank" to index + 1, "uuid" to uuid.toString(), "name" to name, "value" to value)
            }
            ctx.json(mapOf("statId" to statId, "entries" to entries))
        }

        // Public: player profile (limited info)
        app.get("/api/public/player/{uuid}") { ctx ->
            val uuid = try {
                UUID.fromString(ctx.pathParam("uuid"))
            } catch (_: Exception) {
                ctx.status(400).json(mapOf("error" to "Invalid UUID"))
                return@get
            }

            val player = brennon.corePlayerManager.getPlayer(uuid).join()
            if (player.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Player not found"))
                return@get
            }

            val p = player.get()
            ctx.json(mapOf(
                "uuid" to p.uniqueId.toString(),
                "name" to p.name,
                "ranks" to p.getRanks().map { mapOf("id" to it.id, "displayName" to it.displayName) },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "online" to p.isOnline
            ))
        }

        // Public: player profile by name
        app.get("/api/public/player/name/{name}") { ctx ->
            val name = ctx.pathParam("name")
            val player = brennon.corePlayerManager.getPlayer(name).join()
            if (player.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Player not found"))
                return@get
            }

            val p = player.get()
            ctx.json(mapOf(
                "uuid" to p.uniqueId.toString(),
                "name" to p.name,
                "ranks" to p.getRanks().map { mapOf("id" to it.id, "displayName" to it.displayName) },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "online" to p.isOnline
            ))
        }
    }
}
