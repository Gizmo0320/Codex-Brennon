package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class StatsRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/stats/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val stats = brennon.coreStatsManager.getAllStats(uuid).join()
            ctx.json(mapOf("uuid" to uuid.toString(), "stats" to stats))
        }

        app.get("/api/stats/{uuid}/{statId}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val statId = ctx.pathParam("statId")
            val value = brennon.coreStatsManager.getStat(uuid, statId).join()
            ctx.json(mapOf("uuid" to uuid.toString(), "statId" to statId, "value" to value))
        }

        app.get("/api/stats/{uuid}/{statId}/position") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val statId = ctx.pathParam("statId")
            val position = brennon.coreStatsManager.getLeaderboardPosition(uuid, statId).join()
            ctx.json(mapOf("uuid" to uuid.toString(), "statId" to statId, "position" to position))
        }

        app.get("/api/leaderboard/{statId}") { ctx ->
            val statId = ctx.pathParam("statId")
            val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 10
            val leaderboard = brennon.coreStatsManager.getLeaderboard(statId, limit).join()
            val entries = leaderboard.entries.mapIndexed { index, (uuid, value) ->
                val name = brennon.corePlayerManager.getCachedPlayer(uuid)?.name ?: uuid.toString()
                mapOf("rank" to index + 1, "uuid" to uuid.toString(), "name" to name, "value" to value)
            }
            ctx.json(mapOf("statId" to statId, "entries" to entries))
        }

        app.post("/api/stats/increment") { ctx ->
            val body = ctx.bodyAsClass(StatRequest::class.java)
            val uuid = UUID.fromString(body.uuid)
            brennon.coreStatsManager.incrementStat(uuid, body.statId, body.amount).join()
            ctx.json(mapOf("success" to true))
        }

        app.post("/api/stats/set") { ctx ->
            val body = ctx.bodyAsClass(StatSetRequest::class.java)
            val uuid = UUID.fromString(body.uuid)
            brennon.coreStatsManager.setStat(uuid, body.statId, body.value).join()
            ctx.json(mapOf("success" to true))
        }

        app.delete("/api/stats/{uuid}/{statId}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val statId = ctx.pathParam("statId")
            brennon.coreStatsManager.resetStat(uuid, statId).join()
            ctx.json(mapOf("success" to true, "message" to "Stat $statId reset"))
        }

        app.delete("/api/stats/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            brennon.coreStatsManager.resetAllStats(uuid).join()
            ctx.json(mapOf("success" to true, "message" to "All stats reset"))
        }
    }

    data class StatRequest(val uuid: String = "", val statId: String = "", val amount: Double = 0.0)
    data class StatSetRequest(val uuid: String = "", val statId: String = "", val value: Double = 0.0)
}
