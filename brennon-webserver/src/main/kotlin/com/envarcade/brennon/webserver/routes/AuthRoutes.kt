package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.webserver.WebServerConfig
import com.envarcade.brennon.webserver.auth.JwtAuth
import io.javalin.Javalin

class AuthRoutes(
    private val jwtAuth: JwtAuth,
    private val config: WebServerConfig
) {

    fun register(app: Javalin) {
        app.post("/api/auth/login") { ctx ->
            val body = ctx.bodyAsClass(LoginRequest::class.java)

            val token = jwtAuth.authenticate(body.username, body.password)
            if (token == null) {
                ctx.status(401).json(mapOf("error" to "Invalid username or password"))
                return@post
            }

            ctx.json(mapOf("token" to token, "username" to body.username))
        }

        app.post("/api/auth/refresh") { ctx ->
            val authHeader = ctx.header("Authorization") ?: ""
            val token = authHeader.removePrefix("Bearer ").trim()

            val newToken = jwtAuth.refreshToken(token)
            if (newToken == null) {
                ctx.status(401).json(mapOf("error" to "Invalid token"))
                return@post
            }

            ctx.json(mapOf("token" to newToken))
        }

        app.get("/api/auth/me") { ctx ->
            val authHeader = ctx.header("Authorization") ?: ""
            val token = authHeader.removePrefix("Bearer ").trim()

            // API key auth
            if (token == config.apiKey) {
                ctx.json(mapOf("username" to "api", "role" to "admin"))
                return@get
            }

            val username = jwtAuth.getUsername(token)
            if (username == null) {
                ctx.status(401).json(mapOf("error" to "Invalid token"))
                return@get
            }

            ctx.json(mapOf("username" to username, "role" to "admin"))
        }
    }

    data class LoginRequest(val username: String = "", val password: String = "")
}
