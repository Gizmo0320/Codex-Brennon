package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin

class ModuleRoutes(
    private val brennon: Brennon,
    private val extraModules: Map<String, Boolean> = emptyMap()
) {

    fun register(app: Javalin) {
        app.get("/api/modules") { ctx ->
            val modules = mutableMapOf(
                "economy" to brennon.config.modules.economy,
                "punishments" to brennon.config.modules.punishments,
                "ranks" to brennon.config.modules.ranks,
                "serverManager" to brennon.config.modules.serverManager,
                "staffTools" to brennon.config.modules.staffTools,
                "chat" to brennon.config.modules.chat,
                "tickets" to brennon.config.modules.tickets,
                "stats" to brennon.config.modules.stats,
                "gui" to brennon.config.modules.gui
            )
            modules.putAll(extraModules)
            ctx.json(modules)
        }
    }
}
