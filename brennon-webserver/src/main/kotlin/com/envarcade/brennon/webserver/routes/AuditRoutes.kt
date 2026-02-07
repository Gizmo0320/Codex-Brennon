package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.webserver.audit.AuditCategory
import com.envarcade.brennon.webserver.audit.AuditLogger
import io.javalin.Javalin

class AuditRoutes(private val auditLogger: AuditLogger) {

    fun register(app: Javalin) {
        // GET /api/audit?limit=50&offset=0
        app.get("/api/audit") { ctx ->
            val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 50
            val offset = ctx.queryParam("offset")?.toIntOrNull() ?: 0
            val entries = auditLogger.getRecentEntries(limit.coerceIn(1, 200), offset.coerceAtLeast(0))
            ctx.json(entries)
        }

        // GET /api/audit/user/{username}?limit=50
        app.get("/api/audit/user/{username}") { ctx ->
            val username = ctx.pathParam("username")
            val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 50
            val entries = auditLogger.getEntriesByUser(username, limit.coerceIn(1, 200))
            ctx.json(entries)
        }

        // GET /api/audit/category/{category}?limit=50
        app.get("/api/audit/category/{category}") { ctx ->
            val categoryName = ctx.pathParam("category").uppercase()
            val category = try {
                AuditCategory.valueOf(categoryName)
            } catch (_: IllegalArgumentException) {
                ctx.status(400).json(mapOf("error" to "Invalid category: $categoryName"))
                return@get
            }
            val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 50
            val entries = auditLogger.getEntriesByCategory(category, limit.coerceIn(1, 200))
            ctx.json(entries)
        }
    }
}
