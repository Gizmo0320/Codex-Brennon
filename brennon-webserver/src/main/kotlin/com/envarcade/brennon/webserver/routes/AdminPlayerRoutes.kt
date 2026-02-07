package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.webserver.WebServerConfig
import com.envarcade.brennon.webserver.auth.JwtAuth
import com.envarcade.brennon.webserver.auth.RoleMiddleware
import com.google.gson.Gson
import io.javalin.Javalin
import java.util.UUID

class AdminPlayerRoutes(
    private val brennon: Brennon,
    private val jwtAuth: JwtAuth,
    private val config: WebServerConfig
) {

    private val gson = Gson()

    fun register(app: Javalin) {

        // Admin: kick a player via Redis
        app.post("/api/admin/players/{uuid}/kick") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@post
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val body = ctx.bodyAsClass(KickRequest::class.java)

            brennon.redisMessaging.publish(
                Channels.PLAYER_KICK,
                gson.toJson(mapOf("uuid" to uuid.toString(), "reason" to body.reason))
            )
            ctx.json(mapOf("success" to true, "message" to "Kick request sent"))
        }

        // Admin: transfer a player to another server
        app.post("/api/admin/players/{uuid}/transfer") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@post
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val body = ctx.bodyAsClass(TransferRequest::class.java)

            if (body.server.isBlank()) {
                ctx.status(400).json(mapOf("error" to "server is required"))
                return@post
            }

            brennon.coreServerManager.sendPlayer(uuid, body.server).join()
            ctx.json(mapOf("success" to true, "message" to "Player sent to ${body.server}"))
        }

        // Admin: send broadcast
        app.post("/api/admin/broadcast") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@post
            val body = ctx.bodyAsClass(BroadcastRequest::class.java)

            if (body.message.isBlank()) {
                ctx.status(400).json(mapOf("error" to "message is required"))
                return@post
            }

            val payload = mutableMapOf<String, Any>("message" to body.message)
            if (body.server.isNotBlank()) payload["server"] = body.server

            brennon.redisMessaging.publish(Channels.BROADCAST, gson.toJson(payload))
            ctx.json(mapOf("success" to true, "message" to "Broadcast sent"))
        }

        // Admin: full player detail (profile + punishments + economy + stats + tickets)
        app.get("/api/admin/players/{uuid}/full") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@get
            val uuid = UUID.fromString(ctx.pathParam("uuid"))

            val player = brennon.corePlayerManager.getPlayer(uuid).join()
            if (player.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Player not found"))
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

            // Economy
            try {
                result["balance"] = brennon.coreEconomyManager.getBalance(uuid).join()
            } catch (_: Exception) { }

            // Punishments
            try {
                val punishments = brennon.corePunishmentManager.getHistory(uuid).join()
                result["punishments"] = punishments.map { pun ->
                    mapOf(
                        "id" to pun.id,
                        "type" to pun.type.name,
                        "reason" to pun.reason,
                        "issuedBy" to pun.issuer.toString(),
                        "issuedAt" to pun.issuedAt.toString(),
                        "expiresAt" to pun.expiresAt?.toString(),
                        "isPermanent" to pun.isPermanent,
                        "isActive" to pun.isActive,
                        "revokedBy" to pun.revokedBy?.toString()
                    )
                }
            } catch (_: Exception) { }

            // Stats
            if (brennon.config.modules.stats) {
                try {
                    result["stats"] = brennon.coreStatsManager.getAllStats(uuid).join()
                } catch (_: Exception) { }
            }

            // Tickets
            if (brennon.config.modules.tickets) {
                try {
                    val tickets = brennon.coreTicketManager.getPlayerTickets(uuid).join()
                    result["tickets"] = tickets.map { t ->
                        mapOf(
                            "id" to t.id,
                            "subject" to t.subject,
                            "status" to t.status,
                            "priority" to t.priority
                        )
                    }
                } catch (_: Exception) { }
            }

            ctx.json(result)
        }
    }

    data class KickRequest(val reason: String = "Kicked by admin")
    data class TransferRequest(val server: String = "")
    data class BroadcastRequest(val message: String = "", val server: String = "")
}
