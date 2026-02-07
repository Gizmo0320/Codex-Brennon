package com.envarcade.brennon.webserver.auth

import io.javalin.http.Context
import java.util.UUID

/**
 * Role-based access control helpers for route handlers.
 */
object RoleMiddleware {

    /**
     * Requires admin role. Returns true if authorized, false if 403 was sent.
     * Call at the start of admin-only route handlers.
     */
    fun requireAdmin(ctx: Context, jwtAuth: JwtAuth, apiKey: String): Boolean {
        // API key auth is always admin
        val token = extractToken(ctx) ?: return deny(ctx)
        if (token == apiKey) return true

        val role = jwtAuth.getRole(token)
        if (role != "admin") return deny(ctx)
        return true
    }

    /**
     * Requires player or admin role. Returns true if authorized, false if 403 was sent.
     */
    fun requirePlayer(ctx: Context, jwtAuth: JwtAuth, apiKey: String): Boolean {
        val token = extractToken(ctx) ?: return deny(ctx)
        if (token == apiKey) return true

        val role = jwtAuth.getRole(token)
        if (role != "admin" && role != "player") return deny(ctx)
        return true
    }

    /** Extracts the role from the current request's token. */
    fun getAuthRole(ctx: Context, jwtAuth: JwtAuth, apiKey: String): String? {
        val token = extractToken(ctx) ?: return null
        if (token == apiKey) return "admin"
        return jwtAuth.getRole(token)
    }

    /** Extracts the player UUID from the current request's player token. */
    fun getAuthPlayerUuid(ctx: Context, jwtAuth: JwtAuth): UUID? {
        val token = extractToken(ctx) ?: return null
        val uuidStr = jwtAuth.getPlayerUuid(token) ?: return null
        return try { UUID.fromString(uuidStr) } catch (_: Exception) { null }
    }

    /** Extracts the username (subject) from the current request's token. */
    fun getAuthUsername(ctx: Context, jwtAuth: JwtAuth): String? {
        val token = extractToken(ctx) ?: return null
        return jwtAuth.getUsername(token)
    }

    private fun extractToken(ctx: Context): String? {
        val authHeader = ctx.header("Authorization") ?: return null
        if (!authHeader.startsWith("Bearer ")) return null
        return authHeader.removePrefix("Bearer ").trim()
    }

    private fun deny(ctx: Context): Boolean {
        ctx.status(403).json(mapOf("error" to "Forbidden — insufficient permissions"))
        return false
    }
}
