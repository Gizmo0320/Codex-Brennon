package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.time.Duration
import java.util.UUID

class PunishmentRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/punishments/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val punishments = brennon.corePunishmentManager.getHistory(uuid).join()
            ctx.json(punishments.map { p ->
                mapOf(
                    "id" to p.id,
                    "type" to p.type.name,
                    "reason" to p.reason,
                    "issuedBy" to p.issuer.toString(),
                    "issuedAt" to p.issuedAt.toString(),
                    "expiresAt" to p.expiresAt?.toString(),
                    "isPermanent" to p.isPermanent,
                    "isActive" to p.isActive,
                    "revokedBy" to p.revokedBy?.toString()
                )
            })
        }

        app.get("/api/punishments/{uuid}/active") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
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

        app.post("/api/punishments/ban") { ctx ->
            val body = ctx.bodyAsClass(PunishmentRequest::class.java)
            val duration = if (body.duration > 0) Duration.ofMillis(body.duration) else null
            brennon.corePunishmentManager.ban(
                UUID.fromString(body.target), body.reason, duration,
                UUID.fromString(body.issuer)
            ).join()
            ctx.json(mapOf("success" to true, "message" to "Player banned"))
        }

        app.post("/api/punishments/mute") { ctx ->
            val body = ctx.bodyAsClass(PunishmentRequest::class.java)
            val duration = if (body.duration > 0) Duration.ofMillis(body.duration) else null
            brennon.corePunishmentManager.mute(
                UUID.fromString(body.target), body.reason, duration,
                UUID.fromString(body.issuer)
            ).join()
            ctx.json(mapOf("success" to true, "message" to "Player muted"))
        }

        app.post("/api/punishments/kick") { ctx ->
            val body = ctx.bodyAsClass(PunishmentRequest::class.java)
            brennon.corePunishmentManager.kick(
                UUID.fromString(body.target), body.reason,
                UUID.fromString(body.issuer)
            ).join()
            ctx.json(mapOf("success" to true, "message" to "Player kicked"))
        }

        app.post("/api/punishments/warn") { ctx ->
            val body = ctx.bodyAsClass(PunishmentRequest::class.java)
            brennon.corePunishmentManager.warn(
                UUID.fromString(body.target), body.reason,
                UUID.fromString(body.issuer)
            ).join()
            ctx.json(mapOf("success" to true, "message" to "Player warned"))
        }

        app.delete("/api/punishments/unban/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            brennon.corePunishmentManager.unban(uuid, null).join()
            ctx.json(mapOf("success" to true, "message" to "Player unbanned"))
        }

        app.delete("/api/punishments/unmute/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            brennon.corePunishmentManager.unmute(uuid, null).join()
            ctx.json(mapOf("success" to true, "message" to "Player unmuted"))
        }
    }

    data class PunishmentRequest(
        val target: String = "",
        val reason: String = "",
        val issuer: String = "",
        val duration: Long = 0
    )
}
