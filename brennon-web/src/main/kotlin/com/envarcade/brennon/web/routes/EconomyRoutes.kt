package com.envarcade.brennon.web.routes

import com.envarcade.brennon.core.Brennon
import com.google.gson.Gson
import com.google.gson.JsonParser
import spark.Spark
import java.util.UUID

class EconomyRoutes(private val brennon: Brennon) {

    private val gson = Gson()

    fun register() {
        Spark.get("/api/economy/:uuid") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val balance = brennon.coreEconomyManager.getBalance(uuid).join()
            gson.toJson(mapOf("uuid" to uuid.toString(), "balance" to balance))
        }

        Spark.post("/api/economy/deposit") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val uuid = UUID.fromString(body.get("uuid").asString)
            val amount = body.get("amount").asDouble
            brennon.coreEconomyManager.deposit(uuid, amount).join()
            val newBalance = brennon.coreEconomyManager.getBalance(uuid).join()
            gson.toJson(mapOf("success" to true, "balance" to newBalance))
        }

        Spark.post("/api/economy/withdraw") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val uuid = UUID.fromString(body.get("uuid").asString)
            val amount = body.get("amount").asDouble
            brennon.coreEconomyManager.withdraw(uuid, amount).join()
            val newBalance = brennon.coreEconomyManager.getBalance(uuid).join()
            gson.toJson(mapOf("success" to true, "balance" to newBalance))
        }

        Spark.post("/api/economy/set") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val uuid = UUID.fromString(body.get("uuid").asString)
            val amount = body.get("amount").asDouble
            brennon.coreEconomyManager.setBalance(uuid, amount).join()
            gson.toJson(mapOf("success" to true, "balance" to amount))
        }

        Spark.post("/api/economy/transfer") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val from = UUID.fromString(body.get("from").asString)
            val to = UUID.fromString(body.get("to").asString)
            val amount = body.get("amount").asDouble
            brennon.coreEconomyManager.transfer(from, to, amount).join()
            gson.toJson(mapOf("success" to true, "message" to "Transferred $amount"))
        }
    }
}
