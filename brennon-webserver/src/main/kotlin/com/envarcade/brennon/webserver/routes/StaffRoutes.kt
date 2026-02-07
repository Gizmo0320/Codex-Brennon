package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class StaffRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/staff") { ctx ->
            val networkStaff = brennon.staffManager.getNetworkStaff().get()
            ctx.json(networkStaff.map { (uuid, data) ->
                mapOf(
                    "uuid" to uuid.toString(),
                    "name" to (data["name"] ?: "Unknown"),
                    "server" to (data["server"] ?: "Unknown"),
                    "vanished" to (data["vanished"] == "true")
                )
            })
        }

        app.get("/api/staff/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            ctx.json(mapOf(
                "uuid" to uuid.toString(),
                "staffMode" to brennon.staffManager.isInStaffMode(uuid),
                "vanished" to brennon.staffManager.isVanished(uuid)
            ))
        }

        app.post("/api/staff/{uuid}/toggle") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val body = ctx.bodyAsClass(StaffToggleRequest::class.java)
            val enabled = brennon.staffManager.toggleStaffMode(uuid, body.name).get()
            ctx.json(mapOf("success" to true, "staffMode" to enabled))
        }

        app.post("/api/staff/{uuid}/vanish") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val vanished = brennon.staffManager.toggleVanish(uuid).get()
            ctx.json(mapOf("success" to true, "vanished" to vanished))
        }
    }

    data class StaffToggleRequest(val name: String = "")
}
