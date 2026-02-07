package com.envarcade.brennon.core.chat

import com.envarcade.brennon.api.chat.ChatChannel
import com.envarcade.brennon.common.model.ChatChannelData

class CoreChatChannel(private val data: ChatChannelData) : ChatChannel {
    override fun getId(): String = data.id
    override fun getDisplayName(): String = data.displayName
    override fun getFormat(): String = data.format
    override fun getPermission(): String = data.permission
    override fun getSendPermission(): String = data.sendPermission
    override fun isCrossServer(): Boolean = data.isCrossServer
    override fun isDefault(): Boolean = data.isDefault
    override fun getShortcut(): String = data.shortcut
    override fun getRadius(): Int = data.radius
    override fun getServerGroups(): Set<String> = data.serverGroups
}
