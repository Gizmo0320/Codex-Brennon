package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.webserver.luckperms.LuckPermsEditorBridge
import io.javalin.Javalin

class LuckPermsRoutes(
    private val brennon: Brennon,
    private val editorBridge: LuckPermsEditorBridge
) {

    fun register(app: Javalin) {
        app.get("/api/luckperms/status") { ctx ->
            val hook = brennon.luckPermsHook
            val config = brennon.config.luckperms
            ctx.json(mapOf(
                "active" to (hook?.isActive == true),
                "enabled" to config.enabled,
                "syncDirection" to config.syncDirection.name,
                "authority" to config.initialAuthority.name
            ))
        }

        app.post("/api/luckperms/editor") { ctx ->
            val hook = brennon.luckPermsHook
            if (hook == null || !hook.isActive) {
                ctx.status(400).json(mapOf("error" to "LuckPerms is not active on any game server"))
                return@post
            }

            try {
                val url = editorBridge.requestEditorUrl().join()
                ctx.json(mapOf("url" to url))
            } catch (e: Exception) {
                val message = e.cause?.message ?: e.message ?: "Failed to get editor URL"
                ctx.status(502).json(mapOf("error" to message))
            }
        }
    }
}
