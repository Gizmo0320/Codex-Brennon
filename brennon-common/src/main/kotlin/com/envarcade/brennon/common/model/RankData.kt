package com.envarcade.brennon.common.model

data class RankData(
    val id: String,
    var displayName: String = id,
    var prefix: String = "",
    var suffix: String = "",
    var weight: Int = 0,
    var permissions: MutableSet<String> = mutableSetOf(),
    var inheritance: MutableSet<String> = mutableSetOf(),
    var isDefault: Boolean = false,
    var isStaff: Boolean = false,
    var metadata: MutableMap<String, String> = mutableMapOf()
)
