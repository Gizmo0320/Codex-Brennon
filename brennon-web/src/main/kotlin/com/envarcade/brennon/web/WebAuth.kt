package com.envarcade.brennon.web

import com.envarcade.brennon.common.config.WebConfig
import spark.Spark

/**
 * API key authentication filter.
 *
 * Validates the Authorization header on all /api/ routes.
 * Expects: Authorization: Bearer apiKey
 */
class WebAuth(private val config: WebConfig) {

    fun register() {
        Spark.before("/api/*") { request, _ ->
            val authHeader = request.headers("Authorization")
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Spark.halt(401, "{\"error\":\"Missing or invalid Authorization header\"}")
                return@before
            }

            val token = authHeader.removePrefix("Bearer ").trim()
            if (token != config.apiKey) {
                Spark.halt(401, "{\"error\":\"Invalid API key\"}")
            }
        }
    }
}
