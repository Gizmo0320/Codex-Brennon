package com.envarcade.brennon.common.model

import java.time.Instant
import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    var name: String,
    var primaryRank: String = "default",
    var ranks: MutableSet<String> = mutableSetOf("default"),
    var permissions: MutableSet<String> = mutableSetOf(),
    var balance: Double = 0.0,
    var firstJoin: Instant = Instant.now(),
    var lastSeen: Instant = Instant.now(),
    var lastServer: String = "",
    var ipAddress: String = "",
    var playtime: Long = 0L,
    var metadata: MutableMap<String, String> = mutableMapOf()
)

data class PlayerSession(
    val uuid: UUID,
    val name: String,
    val server: String,
    val proxy: String,
    val connectedAt: Instant = Instant.now()
)
