package com.envarcade.brennon.web.routes

import com.envarcade.brennon.core.Brennon
import com.google.gson.Gson
import com.google.gson.JsonParser
import spark.Spark
import java.time.Duration
import java.util.UUID

class PunishmentRoutes(private val brennon: Brennon) {

    private val gson = Gson()

    fun register() {
        Spark.get("/api/punishments/:uuid") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val punishments = brennon.corePunishmentManager.getHistory(uuid).join()
            gson.toJson(punishments.map { p ->
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

        Spark.get("/api/punishments/:uuid/active") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val punishments = brennon.corePunishmentManager.getActivePunishments(uuid).join()
            gson.toJson(punishments.map { p ->
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

        Spark.post("/api/punishments/ban") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val target = UUID.fromString(body.get("target").asString)
            val reason = body.get("reason").asString
            val issuer = UUID.fromString(body.get("issuer").asString)
            val duration = if (body.has("duration") && body.get("duration").asLong > 0)
                Duration.ofMillis(body.get("duration").asLong) else null

            brennon.corePunishmentManager.ban(target, reason, duration, issuer).join()
            gson.toJson(mapOf("success" to true, "message" to "Player banned"))
        }

        Spark.post("/api/punishments/mute") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val target = UUID.fromString(body.get("target").asString)
            val reason = body.get("reason").asString
            val issuer = UUID.fromString(body.get("issuer").asString)
            val duration = if (body.has("duration") && body.get("duration").asLong > 0)
                Duration.ofMillis(body.get("duration").asLong) else null

            brennon.corePunishmentManager.mute(target, reason, duration, issuer).join()
            gson.toJson(mapOf("success" to true, "message" to "Player muted"))
        }

        Spark.delete("/api/punishments/unban/:uuid") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            brennon.corePunishmentManager.unban(uuid, null).join()
            gson.toJson(mapOf("success" to true, "message" to "Player unbanned"))
        }

        Spark.delete("/api/punishments/unmute/:uuid") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            brennon.corePunishmentManager.unmute(uuid, null).join()
            gson.toJson(mapOf("success" to true, "message" to "Player unmuted"))
        }
    }
}
