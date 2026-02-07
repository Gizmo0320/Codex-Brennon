package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class PlayerRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/players/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val player = brennon.corePlayerManager.getPlayer(uuid).join()
            if (player.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Player not found"))
                return@get
            }
            val p = player.get()
            ctx.json(mapOf(
                "uuid" to p.uniqueId.toString(),
                "name" to p.name,
                "ranks" to p.getRanks().map { it.id },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "lastServer" to p.currentServer,
                "online" to p.isOnline
            ))
        }

        app.get("/api/players/name/{name}") { ctx ->
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
                "ranks" to p.getRanks().map { it.id },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "lastServer" to p.currentServer,
                "online" to p.isOnline
            ))
        }

        app.get("/api/players/online") { ctx ->
            val players = brennon.corePlayerManager.getOnlinePlayers().map { p ->
                mapOf(
                    "uuid" to p.uniqueId.toString(),
                    "name" to p.name,
                    "server" to p.currentServer
                )
            }
            ctx.json(players)
        }

        app.get("/api/players/network/sessions") { ctx ->
            val sessions = brennon.corePlayerManager.getNetworkSessions()
            ctx.json(sessions.map { (uuid, server) ->
                mapOf("uuid" to uuid.toString(), "server" to server)
            })
        }

        app.get("/api/players/{uuid}/server") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val server = brennon.corePlayerManager.getNetworkServer(uuid)
            if (server == null) {
                ctx.status(404).json(mapOf("error" to "Player not online"))
                return@get
            }
            ctx.json(mapOf("uuid" to uuid.toString(), "server" to server))
        }

        app.get("/api/players/network/count") { ctx ->
            ctx.json(mapOf("count" to brennon.corePlayerManager.getNetworkOnlineCount()))
        }
    }
}
