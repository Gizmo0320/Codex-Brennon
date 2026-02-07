package com.envarcade.brennon.common.model

import com.envarcade.brennon.api.chat.ChatFilter

data class ChatFilterData(
    val id: String,
    val patterns: List<String> = emptyList(),
    val action: ChatFilter.FilterAction = ChatFilter.FilterAction.BLOCK,
    val replacement: String = "***",
    val enabled: Boolean = true
)
