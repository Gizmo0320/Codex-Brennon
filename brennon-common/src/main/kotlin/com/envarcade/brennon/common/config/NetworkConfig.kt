package com.envarcade.brennon.common.config

data class NetworkConfig(
    val networkId: String = "main",
    val displayName: String = "Main Network",
    val sharing: DataSharingConfig = DataSharingConfig()
)

data class DataSharingConfig(
    val players: DataSharingMode = DataSharingMode.GLOBAL,
    val ranks: DataSharingMode = DataSharingMode.GLOBAL,
    val economy: DataSharingMode = DataSharingMode.GLOBAL,
    val punishments: DataSharingMode = DataSharingMode.NETWORK,
    val tickets: DataSharingMode = DataSharingMode.NETWORK,
    val stats: DataSharingMode = DataSharingMode.NETWORK,
    val chat: DataSharingMode = DataSharingMode.NETWORK,
    val reports: DataSharingMode = DataSharingMode.NETWORK
)

enum class DataSharingMode {
    GLOBAL,
    NETWORK
}
