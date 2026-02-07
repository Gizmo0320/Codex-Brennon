package com.envarcade.brennon.webserver.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.envarcade.brennon.webserver.WebServerConfig
import java.util.Date

class JwtAuth(private val config: WebServerConfig) {

    private val algorithm = Algorithm.HMAC256(config.jwtSecret)
    private val verifier = JWT.require(algorithm)
        .withIssuer("brennon")
        .build()

    fun authenticate(username: String, password: String): String? {
        val user = config.dashboardUsers.firstOrNull {
            it.username == username && it.password == password
        } ?: return null

        return generateToken(user.username)
    }

    fun generateToken(username: String): String {
        val now = Date()
        val expiry = Date(now.time + config.jwtExpirationMinutes * 60 * 1000)

        return JWT.create()
            .withIssuer("brennon")
            .withSubject(username)
            .withClaim("role", "admin")
            .withIssuedAt(now)
            .withExpiresAt(expiry)
            .sign(algorithm)
    }

    fun validateToken(token: String): Boolean {
        return try {
            verifier.verify(token)
            true
        } catch (_: JWTVerificationException) {
            false
        }
    }

    fun getUsername(token: String): String? {
        return try {
            verifier.verify(token).subject
        } catch (_: JWTVerificationException) {
            null
        }
    }

    fun refreshToken(token: String): String? {
        val username = getUsername(token) ?: return null
        val role = getRole(token)
        return if (role == "player") {
            val uuid = getPlayerUuid(token) ?: return null
            generatePlayerToken(uuid, username)
        } else {
            generateToken(username)
        }
    }

    /** Generates a JWT for a linked player with role=player and uuid claim. */
    fun generatePlayerToken(uuid: String, playerName: String): String {
        val now = Date()
        val playerExpiry = config.playerAuth.sessionDurationMinutes
        val expiry = Date(now.time + playerExpiry * 60 * 1000)

        return JWT.create()
            .withIssuer("brennon")
            .withSubject(playerName)
            .withClaim("role", "player")
            .withClaim("uuid", uuid)
            .withIssuedAt(now)
            .withExpiresAt(expiry)
            .sign(algorithm)
    }

    /** Extracts the role claim from a token ("admin" or "player"). */
    fun getRole(token: String): String? {
        return try {
            verifier.verify(token).getClaim("role")?.asString()
        } catch (_: JWTVerificationException) {
            null
        }
    }

    /** Extracts the player UUID claim from a player token. */
    fun getPlayerUuid(token: String): String? {
        return try {
            verifier.verify(token).getClaim("uuid")?.asString()
        } catch (_: JWTVerificationException) {
            null
        }
    }
}
