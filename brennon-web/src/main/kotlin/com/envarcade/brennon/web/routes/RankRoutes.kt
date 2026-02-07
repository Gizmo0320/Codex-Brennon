package com.envarcade.brennon.web.routes

import com.envarcade.brennon.core.Brennon
import com.google.gson.Gson
import com.google.gson.JsonParser
import spark.Spark
import java.util.UUID

class RankRoutes(private val brennon: Brennon) {

    private val gson = Gson()

    fun register() {
        Spark.get("/api/ranks") { _, _ ->
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
            gson.toJson(ranks)
        }

        Spark.get("/api/ranks/:id") { req, _ ->
            val id = req.params("id")
            val rank = brennon.coreRankManager.getRank(id)
            if (rank.isEmpty) {
                Spark.halt(404, """{"error":"Rank not found"}""")
            }
            val r = rank.get()
            gson.toJson(mapOf(
                "id" to r.id,
                "displayName" to r.displayName,
                "prefix" to r.prefix,
                "weight" to r.weight,
                "permissions" to r.permissions,
                "isDefault" to r.isDefault,
                "isStaff" to r.isStaff
            ))
        }

        Spark.post("/api/ranks/set/:uuid/:rankId") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val rankId = req.params("rankId")
            brennon.coreRankManager.setPlayerRank(uuid, rankId).join()
            gson.toJson(mapOf("success" to true, "message" to "Rank set to $rankId"))
        }

        Spark.delete("/api/ranks/remove/:uuid/:rankId") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val rankId = req.params("rankId")
            brennon.coreRankManager.removePlayerRank(uuid, rankId).join()
            gson.toJson(mapOf("success" to true, "message" to "Rank $rankId removed"))
        }
    }
}
