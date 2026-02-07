package com.envarcade.brennon.core.server

import com.envarcade.brennon.api.server.ServerGroupInfo
import com.envarcade.brennon.common.model.ServerGroupDefinition

/**
 * Core implementation of ServerGroupInfo backed by a ServerGroupDefinition.
 */
class CoreServerGroupInfo(
    private val definition: ServerGroupDefinition
) : ServerGroupInfo {

    override fun getId(): String = definition.id
    override fun getDisplayName(): String = definition.displayName
    override fun getJoinPriority(): Int = definition.joinPriority
    override fun isRestricted(): Boolean = definition.restricted
    override fun getPermission(): String = definition.permission
    override fun isFallback(): Boolean = definition.isFallback
    override fun getMaxPlayers(): Int = definition.maxPlayers
}
