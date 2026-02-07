package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class ServerRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/servers") { ctx ->
            val servers = brennon.coreServerManager.getServers().map { s ->
                mapOf(
                    "name" to s.name,
                    "group" to s.group,
                    "playerCount" to s.playerCount,
                    "maxPlayers" to s.maxPlayers,
                    "online" to s.isOnline
                )
            }
            ctx.json(servers)
        }

        app.get("/api/servers/online") { ctx ->
            val count = brennon.coreServerManager.getNetworkPlayerCount()
            val servers = brennon.coreServerManager.getServers().filter { it.isOnline }
            ctx.json(mapOf(
                "totalPlayers" to count,
                "serverCount" to servers.size,
                "servers" to servers.map { s ->
                    mapOf("name" to s.name, "playerCount" to s.playerCount)
                }
            ))
        }

        app.get("/api/servers/{name}") { ctx ->
            val name = ctx.pathParam("name")
            val server = brennon.coreServerManager.getServer(name)
            if (server.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Server not found"))
                return@get
            }
            val s = server.get()
            ctx.json(mapOf(
                "name" to s.name,
                "group" to s.group,
                "playerCount" to s.playerCount,
                "maxPlayers" to s.maxPlayers,
                "online" to s.isOnline
            ))
        }

        app.post("/api/servers/send") { ctx ->
            val body = ctx.bodyAsClass(SendRequest::class.java)
            val uuid = UUID.fromString(body.uuid)
            brennon.coreServerManager.sendPlayer(uuid, body.server).join()
            ctx.json(mapOf("success" to true, "message" to "Player sent to ${body.server}"))
        }

        app.get("/api/servers/groups") { ctx ->
            val groups = brennon.coreServerManager.getGroups().map { g ->
                mapOf(
                    "id" to g.id,
                    "displayName" to g.displayName,
                    "serverCount" to brennon.coreServerManager.getServersByGroup(g.id).size
                )
            }
            ctx.json(groups)
        }

        app.get("/api/servers/groups/{id}") { ctx ->
            val id = ctx.pathParam("id")
            val group = brennon.coreServerManager.getGroup(id)
            if (group.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Group not found"))
                return@get
            }
            val g = group.get()
            val servers = brennon.coreServerManager.getServersByGroup(id)
            ctx.json(mapOf(
                "id" to g.id,
                "displayName" to g.displayName,
                "servers" to servers.map { s ->
                    mapOf(
                        "name" to s.name,
                        "playerCount" to s.playerCount,
                        "online" to s.isOnline
                    )
                }
            ))
        }
    }

    data class SendRequest(val uuid: String = "", val server: String = "")
}
