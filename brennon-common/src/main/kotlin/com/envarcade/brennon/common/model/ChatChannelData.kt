package com.envarcade.brennon.common.model

data class ChatChannelData(
    val id: String,
    val displayName: String,
    val format: String = "<gray><player> <dark_gray>» <white><message>",
    val permission: String = "",
    val sendPermission: String = "",
    val isCrossServer: Boolean = false,
    val isDefault: Boolean = false,
    val shortcut: String = "",
    val radius: Int = -1,
    val serverGroups: Set<String> = emptySet()
)
