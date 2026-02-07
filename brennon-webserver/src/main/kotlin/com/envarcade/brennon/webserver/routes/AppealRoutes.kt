package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.webserver.WebServerConfig
import com.envarcade.brennon.webserver.auth.JwtAuth
import com.envarcade.brennon.webserver.auth.RoleMiddleware
import com.envarcade.brennon.webserver.data.AppealData
import com.envarcade.brennon.webserver.data.AppealRepository
import com.envarcade.brennon.webserver.data.AppealStatus
import io.javalin.Javalin
import java.util.UUID

class AppealRoutes(
    private val brennon: Brennon,
    private val appealRepository: AppealRepository,
    private val jwtAuth: JwtAuth,
    private val config: WebServerConfig
) {

    fun register(app: Javalin) {

        // Player: get own appeals
        app.get("/api/appeals/mine") { ctx ->
            if (!RoleMiddleware.requirePlayer(ctx, jwtAuth, config.apiKey)) return@get
            val uuid = RoleMiddleware.getAuthPlayerUuid(ctx, jwtAuth)
            if (uuid == null) {
                ctx.status(400).json(mapOf("error" to "Player UUID not found in token"))
                return@get
            }
            val appeals = appealRepository.getByPlayer(uuid.toString())
            ctx.json(appeals.map { it.toMap() })
        }

        // Player: create appeal
        app.post("/api/appeals") { ctx ->
            if (!RoleMiddleware.requirePlayer(ctx, jwtAuth, config.apiKey)) return@post
            val uuid = RoleMiddleware.getAuthPlayerUuid(ctx, jwtAuth)
            val playerName = RoleMiddleware.getAuthUsername(ctx, jwtAuth) ?: "Unknown"
            if (uuid == null) {
                ctx.status(400).json(mapOf("error" to "Player UUID not found in token"))
                return@post
            }

            val body = ctx.bodyAsClass(CreateAppealRequest::class.java)
            if (body.punishmentId.isBlank() || body.reason.isBlank()) {
                ctx.status(400).json(mapOf("error" to "punishmentId and reason are required"))
                return@post
            }

            // Verify the punishment exists and belongs to the player
            val punishments = brennon.corePunishmentManager.getHistory(uuid).join()
            val punishment = punishments.firstOrNull { it.id == body.punishmentId }
            if (punishment == null) {
                ctx.status(404).json(mapOf("error" to "Punishment not found or does not belong to you"))
                return@post
            }

            // Check for existing pending appeal on this punishment
            if (appealRepository.hasPendingAppeal(body.punishmentId)) {
                ctx.status(409).json(mapOf("error" to "A pending appeal already exists for this punishment"))
                return@post
            }

            val appeal = AppealData(
                id = UUID.randomUUID().toString(),
                punishmentId = body.punishmentId,
                playerUuid = uuid.toString(),
                playerName = playerName,
                reason = body.reason,
                status = AppealStatus.PENDING,
                staffResponse = null,
                staffUuid = null,
                createdAt = System.currentTimeMillis(),
                resolvedAt = null
            )
            appealRepository.create(appeal)
            ctx.json(mapOf("success" to true, "id" to appeal.id))
        }

        // Admin: get all pending appeals
        app.get("/api/appeals") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@get
            val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 50
            val offset = ctx.queryParam("offset")?.toIntOrNull() ?: 0
            val appeals = appealRepository.getPending(limit, offset)
            ctx.json(appeals.map { it.toMap() })
        }

        // Admin: get appeal by ID
        app.get("/api/appeals/{id}") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@get
            val id = ctx.pathParam("id")
            val appeal = appealRepository.getById(id)
            if (appeal == null) {
                ctx.status(404).json(mapOf("error" to "Appeal not found"))
                return@get
            }

            // Include punishment details
            val punishments = brennon.corePunishmentManager.getHistory(UUID.fromString(appeal.playerUuid)).join()
            val punishment = punishments.firstOrNull { it.id == appeal.punishmentId }

            val result = appeal.toMap().toMutableMap()
            if (punishment != null) {
                result["punishment"] = mapOf(
                    "id" to punishment.id,
                    "type" to punishment.type.name,
                    "reason" to punishment.reason,
                    "issuedAt" to punishment.issuedAt.toString(),
                    "expiresAt" to punishment.expiresAt?.toString(),
                    "isPermanent" to punishment.isPermanent,
                    "isActive" to punishment.isActive
                )
            }
            ctx.json(result)
        }

        // Admin: resolve appeal
        app.put("/api/appeals/{id}/resolve") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@put
            val id = ctx.pathParam("id")
            val body = ctx.bodyAsClass(ResolveAppealRequest::class.java)

            val appeal = appealRepository.getById(id)
            if (appeal == null) {
                ctx.status(404).json(mapOf("error" to "Appeal not found"))
                return@put
            }
            if (appeal.status != AppealStatus.PENDING) {
                ctx.status(400).json(mapOf("error" to "Appeal is already resolved"))
                return@put
            }

            val status = try {
                AppealStatus.valueOf(body.status.uppercase())
            } catch (_: Exception) {
                ctx.status(400).json(mapOf("error" to "Invalid status, must be APPROVED or DENIED"))
                return@put
            }

            val staffUsername = RoleMiddleware.getAuthUsername(ctx, jwtAuth) ?: "admin"
            appealRepository.resolve(id, status, staffUsername, body.response)

            // If approved and it's a ban, auto-unban
            if (status == AppealStatus.APPROVED) {
                try {
                    val playerUuid = UUID.fromString(appeal.playerUuid)
                    val punishments = brennon.corePunishmentManager.getActivePunishments(playerUuid).join()
                    val punishment = punishments.firstOrNull { it.id == appeal.punishmentId }
                    if (punishment != null && punishment.type.name == "BAN") {
                        brennon.corePunishmentManager.unban(playerUuid, null).join()
                    }
                } catch (e: Exception) {
                    println("[Brennon] Auto-unban failed for appeal $id: ${e.message}")
                }
            }

            ctx.json(mapOf("success" to true, "status" to status.name))
        }
    }

    private fun AppealData.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "punishmentId" to punishmentId,
        "playerUuid" to playerUuid,
        "playerName" to playerName,
        "reason" to reason,
        "status" to status.name,
        "staffResponse" to staffResponse,
        "staffUuid" to staffUuid,
        "createdAt" to createdAt,
        "resolvedAt" to resolvedAt
    )

    data class CreateAppealRequest(val punishmentId: String = "", val reason: String = "")
    data class ResolveAppealRequest(val status: String = "", val response: String = "")
}
