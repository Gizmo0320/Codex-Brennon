package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class EconomyRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/economy/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val balance = brennon.coreEconomyManager.getBalance(uuid).join()
            ctx.json(mapOf("uuid" to uuid.toString(), "balance" to balance))
        }

        app.post("/api/economy/deposit") { ctx ->
            val body = ctx.bodyAsClass(EconomyRequest::class.java)
            val uuid = UUID.fromString(body.uuid)
            brennon.coreEconomyManager.deposit(uuid, body.amount).join()
            val newBalance = brennon.coreEconomyManager.getBalance(uuid).join()
            ctx.json(mapOf("success" to true, "balance" to newBalance))
        }

        app.post("/api/economy/withdraw") { ctx ->
            val body = ctx.bodyAsClass(EconomyRequest::class.java)
            val uuid = UUID.fromString(body.uuid)
            brennon.coreEconomyManager.withdraw(uuid, body.amount).join()
            val newBalance = brennon.coreEconomyManager.getBalance(uuid).join()
            ctx.json(mapOf("success" to true, "balance" to newBalance))
        }

        app.post("/api/economy/set") { ctx ->
            val body = ctx.bodyAsClass(EconomyRequest::class.java)
            val uuid = UUID.fromString(body.uuid)
            brennon.coreEconomyManager.setBalance(uuid, body.amount).join()
            ctx.json(mapOf("success" to true, "balance" to body.amount))
        }

        app.post("/api/economy/transfer") { ctx ->
            val body = ctx.bodyAsClass(TransferRequest::class.java)
            val from = UUID.fromString(body.from)
            val to = UUID.fromString(body.to)
            brennon.coreEconomyManager.transfer(from, to, body.amount).join()
            ctx.json(mapOf("success" to true, "message" to "Transferred ${body.amount}"))
        }

        app.get("/api/economy/{uuid}/has/{amount}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val amount = ctx.pathParam("amount").toDouble()
            val has = brennon.coreEconomyManager.has(uuid, amount).join()
            ctx.json(mapOf("uuid" to uuid.toString(), "amount" to amount, "has" to has))
        }
    }

    data class EconomyRequest(val uuid: String = "", val amount: Double = 0.0)
    data class TransferRequest(val from: String = "", val to: String = "", val amount: Double = 0.0)
}
