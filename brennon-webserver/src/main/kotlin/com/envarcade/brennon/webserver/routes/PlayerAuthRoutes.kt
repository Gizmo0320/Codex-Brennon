package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.webserver.WebServerConfig
import com.envarcade.brennon.webserver.auth.JwtAuth
import io.javalin.Javalin

class PlayerAuthRoutes(
    private val brennon: Brennon,
    private val jwtAuth: JwtAuth,
    private val config: WebServerConfig
) {

    fun register(app: Javalin) {
        // Verify a link code and return a player JWT
        app.post("/api/player-auth/verify") { ctx ->
            if (!config.playerAuth.enabled) {
                ctx.status(404).json(mapOf("error" to "Player auth is disabled"))
                return@post
            }

            val body = ctx.bodyAsClass(VerifyRequest::class.java)
            val code = body.code.trim()

            if (code.length != 6) {
                ctx.status(400).json(mapOf("error" to "Invalid code format"))
                return@post
            }

            val link = brennon.playerAuthManager.verifyLinkCode(code)
            if (link == null) {
                ctx.status(401).json(mapOf("error" to "Invalid or expired code"))
                return@post
            }

            val token = jwtAuth.generatePlayerToken(link.uuid.toString(), link.playerName)
            ctx.json(mapOf(
                "token" to token,
                "uuid" to link.uuid.toString(),
                "name" to link.playerName,
                "role" to "player"
            ))
        }

        // Refresh a player JWT
        app.post("/api/player-auth/refresh") { ctx ->
            val authHeader = ctx.header("Authorization") ?: ""
            val token = authHeader.removePrefix("Bearer ").trim()

            val newToken = jwtAuth.refreshToken(token)
            if (newToken == null) {
                ctx.status(401).json(mapOf("error" to "Invalid token"))
                return@post
            }

            ctx.json(mapOf("token" to newToken))
        }

        // Get current player info from token
        app.get("/api/player-auth/me") { ctx ->
            val authHeader = ctx.header("Authorization") ?: ""
            val token = authHeader.removePrefix("Bearer ").trim()

            val role = jwtAuth.getRole(token)
            val username = jwtAuth.getUsername(token)

            if (role == null || username == null) {
                ctx.status(401).json(mapOf("error" to "Invalid token"))
                return@get
            }

            val result = mutableMapOf<String, Any?>(
                "username" to username,
                "role" to role
            )

            if (role == "player") {
                val uuid = jwtAuth.getPlayerUuid(token)
                result["uuid"] = uuid
            }

            ctx.json(result)
        }
    }

    data class VerifyRequest(val code: String = "")
}
