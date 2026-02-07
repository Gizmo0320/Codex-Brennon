package com.envarcade.brennon.web.routes

import com.envarcade.brennon.core.Brennon
import com.google.gson.Gson
import spark.Spark

class ServerRoutes(private val brennon: Brennon) {

    private val gson = Gson()

    fun register() {
        Spark.get("/api/servers") { _, _ ->
            val servers = brennon.coreServerManager.getServers().map { s ->
                mapOf(
                    "name" to s.name,
                    "group" to s.group,
                    "playerCount" to s.playerCount,
                    "maxPlayers" to s.maxPlayers,
                    "online" to s.isOnline
                )
            }
            gson.toJson(servers)
        }

        Spark.get("/api/servers/online") { _, _ ->
            val count = brennon.coreServerManager.getNetworkPlayerCount()
            val servers = brennon.coreServerManager.getServers().filter { it.isOnline }
            gson.toJson(mapOf(
                "totalPlayers" to count,
                "serverCount" to servers.size,
                "servers" to servers.map { s ->
                    mapOf("name" to s.name, "playerCount" to s.playerCount)
                }
            ))
        }

        Spark.get("/api/servers/:name") { req, _ ->
            val name = req.params("name")
            val server = brennon.coreServerManager.getServer(name)
            if (server.isEmpty) {
                Spark.halt(404, """{"error":"Server not found"}""")
            }
            val s = server.get()
            gson.toJson(mapOf(
                "name" to s.name,
                "group" to s.group,
                "playerCount" to s.playerCount,
                "maxPlayers" to s.maxPlayers,
                "online" to s.isOnline
            ))
        }
    }
}
