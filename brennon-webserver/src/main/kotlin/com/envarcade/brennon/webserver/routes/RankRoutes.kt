package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.common.config.LuckPermsSyncDirection
import com.envarcade.brennon.common.model.RankData
import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class RankRoutes(private val brennon: Brennon) {

    private fun isLpAuthority(): Boolean {
        return brennon.luckPermsHook?.isActive == true &&
            brennon.config.luckperms.syncDirection != LuckPermsSyncDirection.BRENNON_TO_LP
    }

    fun register(app: Javalin) {
        app.get("/api/ranks") { ctx ->
            val ranks = brennon.coreRankManager.getRanks().map { r ->
                mapOf(
                    "id" to r.id,
                    "displayName" to r.displayName,
                    "prefix" to r.prefix,
                    "weight" to r.weight,
                    "permissions" to r.permissions,
                    "isDefault" to r.isDefault,
                    "isStaff" to r.isStaff
                )
            }
            ctx.json(ranks)
        }

        app.get("/api/ranks/{id}") { ctx ->
            val id = ctx.pathParam("id")
            val rank = brennon.coreRankManager.getRank(id)
            if (rank.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Rank not found"))
                return@get
            }
            val r = rank.get()
            ctx.json(mapOf(
                "id" to r.id,
                "displayName" to r.displayName,
                "prefix" to r.prefix,
                "weight" to r.weight,
                "permissions" to r.permissions,
                "isDefault" to r.isDefault,
                "isStaff" to r.isStaff
            ))
        }

        app.post("/api/ranks") { ctx ->
            if (isLpAuthority()) {
                ctx.status(403).json(mapOf("error" to "Rank editing is disabled — LuckPerms is the authority. Use the LP web editor."))
                return@post
            }
            val body = ctx.bodyAsClass(RankCreateRequest::class.java)
            val data = RankData(
                id = body.id,
                displayName = body.displayName.ifBlank { body.id },
                prefix = body.prefix,
                weight = body.weight,
                permissions = body.permissions.toMutableSet(),
                isDefault = body.isDefault,
                isStaff = body.isStaff
            )
            brennon.coreRankManager.saveRank(data).join()
            ctx.json(mapOf("success" to true, "id" to data.id))
        }

        app.put("/api/ranks/{id}") { ctx ->
            if (isLpAuthority()) {
                ctx.status(403).json(mapOf("error" to "Rank editing is disabled — LuckPerms is the authority. Use the LP web editor."))
                return@put
            }
            val id = ctx.pathParam("id")
            val body = ctx.bodyAsClass(RankCreateRequest::class.java)
            val data = RankData(
                id = id,
                displayName = body.displayName.ifBlank { id },
                prefix = body.prefix,
                weight = body.weight,
                permissions = body.permissions.toMutableSet(),
                isDefault = body.isDefault,
                isStaff = body.isStaff
            )
            brennon.coreRankManager.saveRank(data).join()
            ctx.json(mapOf("success" to true, "id" to id))
        }

        app.delete("/api/ranks/{id}") { ctx ->
            if (isLpAuthority()) {
                ctx.status(403).json(mapOf("error" to "Rank editing is disabled — LuckPerms is the authority. Use the LP web editor."))
                return@delete
            }
            val id = ctx.pathParam("id")
            brennon.coreRankManager.deleteRank(id).join()
            ctx.json(mapOf("success" to true, "message" to "Rank $id deleted"))
        }

        app.post("/api/ranks/set/{uuid}/{rankId}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val rankId = ctx.pathParam("rankId")
            brennon.coreRankManager.setPlayerRank(uuid, rankId).join()
            ctx.json(mapOf("success" to true, "message" to "Rank set to $rankId"))
        }

        app.post("/api/ranks/add/{uuid}/{rankId}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val rankId = ctx.pathParam("rankId")
            brennon.coreRankManager.addPlayerRank(uuid, rankId).join()
            ctx.json(mapOf("success" to true, "message" to "Rank $rankId added"))
        }

        app.delete("/api/ranks/remove/{uuid}/{rankId}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val rankId = ctx.pathParam("rankId")
            brennon.coreRankManager.removePlayerRank(uuid, rankId).join()
            ctx.json(mapOf("success" to true, "message" to "Rank $rankId removed"))
        }

        app.post("/api/ranks/reload") { ctx ->
            brennon.coreRankManager.reload().join()
            ctx.json(mapOf("success" to true, "message" to "Ranks reloaded"))
        }
    }

    data class RankCreateRequest(
        val id: String = "",
        val displayName: String = "",
        val prefix: String = "",
        val weight: Int = 0,
        val permissions: List<String> = emptyList(),
        val isDefault: Boolean = false,
        val isStaff: Boolean = false
    )
}
