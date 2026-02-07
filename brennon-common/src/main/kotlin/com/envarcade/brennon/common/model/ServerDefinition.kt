package com.envarcade.brennon.common.model

/**
 * A registered server definition in the server registry.
 *
 * This represents the static declaration that a server exists at a given
 * address. Live status (online, player count) is tracked separately by
 * the heartbeat system in CoreServerManager.
 */
data class ServerDefinition(
    val name: String,
    val group: String,
    val host: String,
    val port: Int,
    val maxPlayers: Int = 100,
    val restricted: Boolean = false,
    val permission: String = "",
    val autoRegistered: Boolean = false,
    val addedBy: String = "system",
    val addedAt: Long = System.currentTimeMillis()
) {
    val address: String get() = "$host:$port"
}
