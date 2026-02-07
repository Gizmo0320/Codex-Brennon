package com.envarcade.brennon.common.config

import com.envarcade.brennon.common.model.ChatChannelData
import com.envarcade.brennon.common.model.ChatFilterData

data class BrennonConfig(
    val serverName: String = "unknown",
    val serverGroup: String = "default",
    val serverHost: String = "",
    val serverPort: Int = 25565,
    val network: NetworkConfig = NetworkConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val redis: RedisConfig = RedisConfig(),
    val modules: ModulesConfig = ModulesConfig(),
    val serverRegistry: ServerRegistryConfig = ServerRegistryConfig(),
    val chat: ChatConfig = ChatConfig(),
    val discord: DiscordConfig = DiscordConfig(),
    val web: WebConfig = WebConfig(),
    val luckperms: LuckPermsConfig = LuckPermsConfig()
)

data class DatabaseConfig(
    val driver: DatabaseDriver = DatabaseDriver.MONGODB,
    val host: String = "localhost",
    val port: Int = 27017,
    val database: String = "brennon",
    val username: String = "",
    val password: String = "",
    val poolSize: Int = 10,
    val uri: String = ""
)

enum class DatabaseDriver {
    MONGODB,
    MYSQL,
    MARIADB,
    POSTGRESQL
}

data class RedisConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String = "",
    val database: Int = 0,
    val poolSize: Int = 8,
    val timeout: Int = 3000,
    val channelPrefix: String = "brennon:"
)

data class ModulesConfig(
    val economy: Boolean = true,
    val punishments: Boolean = true,
    val ranks: Boolean = true,
    val serverManager: Boolean = true,
    val staffTools: Boolean = true,
    val chat: Boolean = true,
    val tickets: Boolean = true,
    val stats: Boolean = true,
    val gui: Boolean = true
)

data class ChatConfig(
    val channels: List<ChatChannelData> = listOf(
        ChatChannelData(
            id = "global",
            displayName = "Global",
            format = "<rank_prefix> <player> <dark_gray>\u00BB <white><message>",
            isCrossServer = true,
            isDefault = true,
            shortcut = "g",
            radius = -1
        ),
        ChatChannelData(
            id = "staff",
            displayName = "Staff",
            format = "<dark_gray>[<red>Staff</red>] <player> <dark_gray>\u00BB <gray><message>",
            permission = "brennon.chat.staff",
            sendPermission = "brennon.chat.staff",
            isCrossServer = true,
            shortcut = "sc",
            radius = -1
        ),
        ChatChannelData(
            id = "trade",
            displayName = "Trade",
            format = "<dark_gray>[<gold>Trade</gold>] <player> <dark_gray>\u00BB <yellow><message>",
            isCrossServer = true,
            shortcut = "tc",
            radius = -1
        ),
        ChatChannelData(
            id = "local",
            displayName = "Local",
            format = "<gray>[Local] <player> <dark_gray>\u00BB <white><message>",
            isCrossServer = false,
            shortcut = "l",
            radius = 100
        )
    ),
    val filters: List<ChatFilterData> = emptyList(),
    val defaultChannel: String = "global",
    val localRadius: Int = 100
)

data class DiscordConfig(
    val enabled: Boolean = false,
    val token: String = "",
    val guildId: String = "",
    val chatChannelId: String = "",
    val staffChannelId: String = "",
    val alertChannelId: String = "",
    val syncChat: Boolean = true,
    val syncPunishments: Boolean = true,
    val showPlayerCount: Boolean = true
)

data class ServerRegistryConfig(
    val autoRegistration: Boolean = true,
    val autoUnregister: Boolean = true,
    val unregisterTimeoutMs: Long = 60_000,
    val fallbackGroup: String = "lobby"
)

data class WebConfig(
    val enabled: Boolean = false,
    val port: Int = 8080,
    val apiKey: String = "change-me",
    val corsOrigins: String = "*"
)

data class LuckPermsConfig(
    val enabled: Boolean = true,
    val syncDirection: LuckPermsSyncDirection = LuckPermsSyncDirection.BIDIRECTIONAL,
    val fullSyncOnStartup: Boolean = true,
    val initialAuthority: LuckPermsAuthority = LuckPermsAuthority.BRENNON,
    val syncPrefixSuffix: Boolean = true,
    val syncWeight: Boolean = true,
    val syncInheritance: Boolean = true,
    val groupPrefix: String = "",
    val delegatePermissions: Boolean = true
)

enum class LuckPermsSyncDirection {
    BRENNON_TO_LP,
    LP_TO_BRENNON,
    BIDIRECTIONAL
}

enum class LuckPermsAuthority {
    BRENNON,
    LUCKPERMS
}
