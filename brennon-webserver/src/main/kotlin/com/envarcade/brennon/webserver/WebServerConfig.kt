package com.envarcade.brennon.webserver

import com.envarcade.brennon.common.config.DatabaseConfig
import com.envarcade.brennon.common.config.ModulesConfig
import com.envarcade.brennon.common.config.NetworkConfig
import com.envarcade.brennon.common.config.RedisConfig

data class WebServerConfig(
    // Infrastructure (reuse types from brennon-common)
    val database: DatabaseConfig = DatabaseConfig(),
    val redis: RedisConfig = RedisConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val modules: ModulesConfig = ModulesConfig(),

    // Webserver-specific
    val port: Int = 8080,
    val apiKey: String = "change-me",
    val jwtSecret: String = "change-me-to-a-random-secret",
    val jwtExpirationMinutes: Long = 480,
    val wsEnabled: Boolean = true,
    val dashboardUsers: List<DashboardUser> = listOf(
        DashboardUser("admin", "admin")
    ),
    val audit: AuditConfig = AuditConfig(),
    val playerAuth: PlayerAuthConfig = PlayerAuthConfig(),
    val pterodactyl: PterodactylConfig = PterodactylConfig()
)

data class AuditConfig(
    val enabled: Boolean = true,
    val logReads: Boolean = false,
    val logToDatabase: Boolean = true,
    val logToFile: Boolean = true
)

data class DashboardUser(
    val username: String,
    val password: String
)

data class PlayerAuthConfig(
    val enabled: Boolean = true,
    val sessionDurationMinutes: Long = 1440 // 24 hours
)

data class PterodactylConfig(
    val enabled: Boolean = false,
    val apiUrl: String = "https://panel.example.com",
    val apiKey: String = "",
    val serverMappings: Map<String, String> = emptyMap() // brennon server name → pterodactyl server ID
)
