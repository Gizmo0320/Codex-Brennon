package com.envarcade.brennon.web.routes

import com.envarcade.brennon.core.Brennon
import com.google.gson.Gson
import com.google.gson.JsonParser
import spark.Spark
import java.util.UUID

class StatsRoutes(private val brennon: Brennon) {

    private val gson = Gson()

    fun register() {
        Spark.get("/api/stats/:uuid") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val stats = brennon.coreStatsManager.getAllStats(uuid).join()
            gson.toJson(mapOf("uuid" to uuid.toString(), "stats" to stats))
        }

        Spark.get("/api/stats/:uuid/:statId") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val statId = req.params("statId")
            val value = brennon.coreStatsManager.getStat(uuid, statId).join()
            gson.toJson(mapOf("uuid" to uuid.toString(), "statId" to statId, "value" to value))
        }

        Spark.get("/api/leaderboard/:statId") { req, _ ->
            val statId = req.params("statId")
            val limit = req.queryParamOrDefault("limit", "10").toInt()
            val leaderboard = brennon.coreStatsManager.getLeaderboard(statId, limit).join()
            val entries = leaderboard.entries.mapIndexed { index, (uuid, value) ->
                val name = brennon.corePlayerManager.getCachedPlayer(uuid)?.name ?: uuid.toString()
                mapOf("rank" to index + 1, "uuid" to uuid.toString(), "name" to name, "value" to value)
            }
            gson.toJson(mapOf("statId" to statId, "entries" to entries))
        }

        Spark.post("/api/stats/increment") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val uuid = UUID.fromString(body.get("uuid").asString)
            val statId = body.get("statId").asString
            val amount = body.get("amount").asDouble

            brennon.coreStatsManager.incrementStat(uuid, statId, amount).join()
            gson.toJson(mapOf("success" to true))
        }

        Spark.post("/api/stats/set") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val uuid = UUID.fromString(body.get("uuid").asString)
            val statId = body.get("statId").asString
            val value = body.get("value").asDouble

            brennon.coreStatsManager.setStat(uuid, statId, value).join()
            gson.toJson(mapOf("success" to true))
        }
    }
}
