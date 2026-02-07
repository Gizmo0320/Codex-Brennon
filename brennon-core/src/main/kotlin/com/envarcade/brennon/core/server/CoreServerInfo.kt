package com.envarcade.brennon.core.server

import com.envarcade.brennon.api.server.ServerInfo

/**
 * Core implementation of ServerInfo backed by Redis heartbeat data.
 */
data class CoreServerInfo(
    private val name: String,
    private val group: String,
    private var playerCount: Int = 0,
    private val maxPlayers: Int = 100,
    private var online: Boolean = true,
    private val motd: String = "",
    private val whitelisted: Boolean = false,
    private var lastHeartbeat: Long = System.currentTimeMillis()
) : ServerInfo {

    override fun getName(): String = name
    override fun getGroup(): String = group
    override fun getPlayerCount(): Int = playerCount
    override fun getMaxPlayers(): Int = maxPlayers
    override fun isOnline(): Boolean = online && (System.currentTimeMillis() - lastHeartbeat < 30_000)
    override fun getMotd(): String = motd
    override fun isWhitelisted(): Boolean = whitelisted

    /**
     * Updates heartbeat data from a Redis status message.
     */
    fun updateHeartbeat(players: Int, isOnline: Boolean) {
        this.playerCount = players
        this.online = isOnline
        this.lastHeartbeat = System.currentTimeMillis()
    }
}
