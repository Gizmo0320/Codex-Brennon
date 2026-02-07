package com.envarcade.brennon.web.routes

import com.envarcade.brennon.core.Brennon
import com.google.gson.Gson
import spark.Spark
import java.util.UUID

class PlayerRoutes(private val brennon: Brennon) {

    private val gson = Gson()

    fun register() {
        Spark.get("/api/players/:uuid") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val player = brennon.corePlayerManager.getPlayer(uuid).join()
            if (player.isEmpty) {
                Spark.halt(404, """{"error":"Player not found"}""")
            }
            val p = player.get()
            gson.toJson(mapOf(
                "uuid" to p.uniqueId.toString(),
                "name" to p.name,
                "ranks" to p.getRanks().map { it.id },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "lastServer" to p.currentServer,
                "online" to p.isOnline
            ))
        }

        Spark.get("/api/players/name/:name") { req, _ ->
            val name = req.params("name")
            val player = brennon.corePlayerManager.getPlayer(name).join()
            if (player.isEmpty) {
                Spark.halt(404, """{"error":"Player not found"}""")
            }
            val p = player.get()
            gson.toJson(mapOf(
                "uuid" to p.uniqueId.toString(),
                "name" to p.name,
                "ranks" to p.getRanks().map { it.id },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "lastServer" to p.currentServer,
                "online" to p.isOnline
            ))
        }

        Spark.get("/api/players/online") { _, _ ->
            val players = brennon.corePlayerManager.getOnlinePlayers().map { p ->
                mapOf(
                    "uuid" to p.uniqueId.toString(),
                    "name" to p.name,
                    "server" to p.currentServer
                )
            }
            gson.toJson(players)
        }
    }
}
