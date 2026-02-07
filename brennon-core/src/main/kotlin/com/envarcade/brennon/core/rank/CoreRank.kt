package com.envarcade.brennon.core.rank

import com.envarcade.brennon.api.rank.Rank
import com.envarcade.brennon.common.model.RankData
import com.envarcade.brennon.common.util.TextUtil
import net.kyori.adventure.text.Component

/**
 * Core implementation of the Rank interface backed by RankData.
 */
class CoreRank(private val data: RankData) : Rank {

    /** Cached set of all effective permissions (including inherited). */
    private var effectivePermissions: Set<String>? = null

    override fun getId(): String = data.id

    override fun getDisplayName(): String = data.displayName

    override fun getPrefix(): Component = TextUtil.parse(data.prefix)

    override fun getSuffix(): Component = TextUtil.parse(data.suffix)

    override fun getWeight(): Int = data.weight

    override fun getPermissions(): Set<String> = data.permissions.toSet()

    override fun getInheritance(): Set<String> = data.inheritance.toSet()

    override fun hasPermission(permission: String): Boolean {
        val perms = getEffectivePermissions()

        // Check wildcard
        if (perms.contains("*")) return true

        // Check negated
        if (perms.contains("-$permission")) return false

        // Check exact match
        if (perms.contains(permission)) return true

        // Check wildcard nodes (e.g., "brennon.admin.*" matches "brennon.admin.ban")
        val parts = permission.split(".")
        for (i in parts.indices) {
            val wildcard = parts.subList(0, i + 1).joinToString(".") + ".*"
            if (perms.contains(wildcard)) return true
        }

        return false
    }

    override fun isDefault(): Boolean = data.isDefault

    override fun isStaff(): Boolean = data.isStaff

    /**
     * Gets the raw data model (for persistence).
     */
    fun getData(): RankData = data

    /**
     * Gets all effective permissions including inherited ones.
     * This is resolved by the CoreRankManager after all ranks are loaded.
     */
    fun getEffectivePermissions(): Set<String> = effectivePermissions ?: data.permissions

    /**
     * Sets the resolved effective permissions (called by CoreRankManager).
     */
    fun setEffectivePermissions(permissions: Set<String>) {
        effectivePermissions = permissions
    }

    /**
     * Invalidates the cached effective permissions.
     */
    fun invalidateCache() {
        effectivePermissions = null
    }
}
