package com.envarcade.brennon.common.model

/**
 * A server group definition.
 *
 * Groups organize servers into logical categories (lobby, survival, pvp, etc.)
 * and can define group-level properties like join priority and access restrictions.
 */
data class ServerGroupDefinition(
    val id: String,
    val displayName: String = id,
    val joinPriority: Int = 0,
    val restricted: Boolean = false,
    val permission: String = "",
    val isFallback: Boolean = false,
    val maxPlayers: Int = -1
)
