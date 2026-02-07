package com.envarcade.brennon.core.chat

import com.envarcade.brennon.api.chat.ChatFilter
import com.envarcade.brennon.common.model.ChatFilterData
import java.util.regex.Pattern

class CoreChatFilter(private val data: ChatFilterData) : ChatFilter {

    private val compiledPatterns: List<Pattern> = data.patterns.map { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

    override fun getId(): String = data.id
    override fun getPatterns(): List<Pattern> = compiledPatterns
    override fun getAction(): ChatFilter.FilterAction = data.action
    override fun getReplacement(): String = data.replacement
    override fun isEnabled(): Boolean = data.enabled
}
