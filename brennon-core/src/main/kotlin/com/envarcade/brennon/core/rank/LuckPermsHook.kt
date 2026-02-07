package com.envarcade.brennon.core.rank

import com.envarcade.brennon.common.config.LuckPermsConfig
import com.envarcade.brennon.common.config.LuckPermsAuthority
import com.envarcade.brennon.common.config.LuckPermsSyncDirection
import com.envarcade.brennon.common.model.RankData
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.RankSyncEvent
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.event.node.NodeAddEvent
import net.luckperms.api.event.node.NodeRemoveEvent
import net.luckperms.api.model.group.Group
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import net.luckperms.api.node.types.InheritanceNode
import net.luckperms.api.node.types.PermissionNode
import net.luckperms.api.node.types.PrefixNode
import net.luckperms.api.node.types.SuffixNode
import net.luckperms.api.node.types.WeightNode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Bidirectional integration hook between Brennon's rank system and LuckPerms.
 *
 * Handles:
 * - Detecting whether LuckPerms is installed
 * - Pushing Brennon rank changes to LuckPerms groups
 * - Listening for LuckPerms changes and reflecting them in Brennon
 * - Loop prevention via pending operation keys
 */
class LuckPermsHook(
    private val config: LuckPermsConfig,
    private val rankManager: CoreRankManager,
    private val eventBus: CoreEventBus
) {

    private var luckPerms: LuckPerms? = null

    /** Whether LP was detected and successfully hooked */
    var isActive: Boolean = false
        private set

    /**
     * Pending operation keys to prevent sync loops.
     * When we push a change to LP, we add a key here. When LP fires an event
     * for that change, we check this set — if found, we consume the key and skip
     * processing (it was our own write).
     */
    private val pendingOps = ConcurrentHashMap<String, Long>()

    // ============================================================
    // Initialization
    // ============================================================

    /**
     * Attempts to detect and hook into LuckPerms.
     * @return true if LP was found and hooked successfully
     */
    fun initialize(): Boolean {
        return try {
            val lp = LuckPermsProvider.get()
            this.luckPerms = lp
            isActive = true

            // Register LP event listeners for LP -> Brennon sync
            if (config.syncDirection != LuckPermsSyncDirection.BRENNON_TO_LP) {
                registerLuckPermsListeners(lp)
            }

            // Full sync on startup
            if (config.fullSyncOnStartup) {
                performFullSync()
            }

            println("[Brennon] LuckPerms detected and hooked successfully. Sync: ${config.syncDirection}")
            true
        } catch (e: IllegalStateException) {
            // LuckPerms not installed
            println("[Brennon] LuckPerms not found — using built-in permission system.")
            false
        }
    }

    /**
     * Shuts down the hook and clears pending operations.
     */
    fun shutdown() {
        pendingOps.clear()
        isActive = false
        luckPerms = null
    }

    // ============================================================
    // Brennon -> LuckPerms Sync
    // ============================================================

    /**
     * Pushes a Brennon rank definition to a LuckPerms group.
     * Creates the group if it doesn't exist, then updates all properties.
     */
    fun pushRankToLuckPerms(rank: CoreRank) {
        if (!isActive || config.syncDirection == LuckPermsSyncDirection.LP_TO_BRENNON) return
        val lp = luckPerms ?: return
        val data = rank.getData()
        val groupName = toLuckPermsGroupName(data.id)

        addPendingOp("group:$groupName")

        lp.groupManager.createAndLoadGroup(groupName).thenAcceptAsync { group ->
            // Clear existing nodes and rebuild
            group.data().clear()

            // Permission nodes
            for (perm in data.permissions) {
                if (perm.startsWith("-")) {
                    group.data().add(PermissionNode.builder(perm.substring(1)).value(false).build())
                } else {
                    group.data().add(PermissionNode.builder(perm).build())
                }
            }

            // Prefix/suffix
            if (config.syncPrefixSuffix) {
                if (data.prefix.isNotBlank()) {
                    group.data().add(PrefixNode.builder(data.prefix, data.weight).build())
                }
                if (data.suffix.isNotBlank()) {
                    group.data().add(SuffixNode.builder(data.suffix, data.weight).build())
                }
            }

            // Weight
            if (config.syncWeight) {
                group.data().add(WeightNode.builder(data.weight).build())
            }

            // Inheritance
            if (config.syncInheritance) {
                for (parent in data.inheritance) {
                    val parentGroupName = toLuckPermsGroupName(parent)
                    group.data().add(InheritanceNode.builder(parentGroupName).build())
                }
            }

            lp.groupManager.saveGroup(group)
            eventBus.publish(RankSyncEvent(data.id, "brennon_to_lp"))
        }
    }

    /**
     * Deletes a LuckPerms group corresponding to a Brennon rank.
     */
    fun deleteGroupFromLuckPerms(rankId: String) {
        if (!isActive || config.syncDirection == LuckPermsSyncDirection.LP_TO_BRENNON) return
        val lp = luckPerms ?: return
        val groupName = toLuckPermsGroupName(rankId)

        addPendingOp("group:$groupName:delete")

        lp.groupManager.loadGroup(groupName).thenAcceptAsync { optional ->
            optional.ifPresent { group ->
                lp.groupManager.deleteGroup(group)
            }
        }
    }

    /**
     * Sets a player's primary rank in LuckPerms.
     * Removes all existing parent groups and sets the new one as primary.
     */
    fun pushPlayerRankToLuckPerms(uuid: UUID, rankId: String) {
        if (!isActive || config.syncDirection == LuckPermsSyncDirection.LP_TO_BRENNON) return
        val lp = luckPerms ?: return
        val groupName = toLuckPermsGroupName(rankId)

        addPendingOp("user:$uuid:$groupName")

        lp.userManager.loadUser(uuid).thenAcceptAsync { user ->
            // Remove all existing inheritance nodes
            val existingInheritance = user.getNodes().filterIsInstance<InheritanceNode>()
            for (node in existingInheritance) {
                user.data().remove(node)
            }

            // Add the new primary group
            user.data().add(InheritanceNode.builder(groupName).build())

            lp.userManager.saveUser(user)
        }
    }

    /**
     * Adds a secondary rank to a player in LuckPerms.
     */
    fun addPlayerRankToLuckPerms(uuid: UUID, rankId: String) {
        if (!isActive || config.syncDirection == LuckPermsSyncDirection.LP_TO_BRENNON) return
        val lp = luckPerms ?: return
        val groupName = toLuckPermsGroupName(rankId)

        addPendingOp("user:$uuid:$groupName")

        lp.userManager.loadUser(uuid).thenAcceptAsync { user ->
            user.data().add(InheritanceNode.builder(groupName).build())
            lp.userManager.saveUser(user)
        }
    }

    /**
     * Removes a rank from a player in LuckPerms.
     */
    fun removePlayerRankFromLuckPerms(uuid: UUID, rankId: String) {
        if (!isActive || config.syncDirection == LuckPermsSyncDirection.LP_TO_BRENNON) return
        val lp = luckPerms ?: return
        val groupName = toLuckPermsGroupName(rankId)

        addPendingOp("user:$uuid:$groupName:remove")

        lp.userManager.loadUser(uuid).thenAcceptAsync { user ->
            user.data().remove(InheritanceNode.builder(groupName).build())
            lp.userManager.saveUser(user)
        }
    }

    /**
     * Syncs a player's Brennon ranks to LuckPerms on join.
     * Ensures LP user groups match Brennon rank assignments.
     */
    fun syncPlayerToLuckPerms(uuid: UUID, ranks: Set<String>, primaryRank: String) {
        if (!isActive) return
        val lp = luckPerms ?: return

        addPendingOp("user:$uuid:sync")

        lp.userManager.loadUser(uuid).thenAcceptAsync { user ->
            // Remove all existing inheritance nodes
            val existingInheritance = user.getNodes().filterIsInstance<InheritanceNode>()
            for (node in existingInheritance) {
                user.data().remove(node)
            }

            // Add all Brennon ranks as LP groups
            for (rankId in ranks) {
                val groupName = toLuckPermsGroupName(rankId)
                user.data().add(InheritanceNode.builder(groupName).build())
            }

            // Ensure primary rank is present
            val primaryGroupName = toLuckPermsGroupName(primaryRank)
            user.data().add(InheritanceNode.builder(primaryGroupName).build())

            // Set primary group
            user.primaryGroup = primaryGroupName

            lp.userManager.saveUser(user)
        }
    }

    // ============================================================
    // LuckPerms -> Brennon Sync (Event Listeners)
    // ============================================================

    private fun registerLuckPermsListeners(lp: LuckPerms) {
        val eventBus = lp.eventBus

        // Listen for nodes being added to groups or users
        eventBus.subscribe(NodeAddEvent::class.java) { event ->
            handleNodeEvent(event.target, event.node, isAdd = true)
        }

        // Listen for nodes being removed from groups or users
        eventBus.subscribe(NodeRemoveEvent::class.java) { event ->
            handleNodeEvent(event.target, event.node, isAdd = false)
        }
    }

    private fun handleNodeEvent(target: Any, node: Node, isAdd: Boolean) {
        // Only process inheritance node changes (rank assignments)
        if (node !is InheritanceNode) return

        when (target) {
            is User -> handleUserInheritanceChange(target, node, isAdd)
            is Group -> handleGroupChange(target)
        }
    }

    private fun handleUserInheritanceChange(user: User, node: InheritanceNode, isAdd: Boolean) {
        val uuid = user.uniqueId
        val groupName = node.groupName
        val opKey = if (isAdd) "user:$uuid:$groupName" else "user:$uuid:$groupName:remove"

        // Check if this was our own operation (loop prevention)
        if (consumePendingOp(opKey) || consumePendingOp("user:$uuid:sync")) return

        val rankId = fromLuckPermsGroupName(groupName)

        // Check if this rank exists in Brennon
        if (rankManager.getCoreRank(rankId) == null) return

        // Suppress LP sync to prevent loop
        rankManager.suppressLuckPermsSync = true
        try {
            if (isAdd) {
                rankManager.addPlayerRank(uuid, rankId)
            } else {
                rankManager.removePlayerRank(uuid, rankId)
            }
        } finally {
            rankManager.suppressLuckPermsSync = false
        }
    }

    private fun handleGroupChange(group: Group) {
        val groupName = group.name
        val opKey = "group:$groupName"

        // Check if this was our own operation
        if (consumePendingOp(opKey)) return

        val rankId = fromLuckPermsGroupName(groupName)

        // Convert LP group to Brennon rank data
        val rankData = groupToRankData(group, rankId) ?: return

        // Suppress LP sync to prevent loop
        rankManager.suppressLuckPermsSync = true
        try {
            rankManager.saveRank(rankData)
        } finally {
            rankManager.suppressLuckPermsSync = false
        }
    }

    // ============================================================
    // Full Sync
    // ============================================================

    /**
     * Performs a full sync between Brennon and LuckPerms on startup.
     */
    private fun performFullSync() {
        val lp = luckPerms ?: return

        when (config.initialAuthority) {
            LuckPermsAuthority.BRENNON -> {
                // Push all Brennon ranks to LP
                println("[Brennon] Full sync: Brennon -> LuckPerms (${rankManager.getRanks().size} ranks)")
                for (rank in rankManager.getRanks()) {
                    val coreRank = rankManager.getCoreRank(rank.id) ?: continue
                    pushRankToLuckPerms(coreRank)
                }
            }
            LuckPermsAuthority.LUCKPERMS -> {
                // Pull all LP groups into Brennon
                println("[Brennon] Full sync: LuckPerms -> Brennon")
                lp.groupManager.loadAllGroups().thenRunAsync {
                    val groups = lp.groupManager.loadedGroups
                    println("[Brennon] Importing ${groups.size} LuckPerms groups")

                    rankManager.suppressLuckPermsSync = true
                    try {
                        for (group in groups) {
                            val rankId = fromLuckPermsGroupName(group.name)
                            val rankData = groupToRankData(group, rankId) ?: continue
                            rankManager.saveRank(rankData)
                        }
                    } finally {
                        rankManager.suppressLuckPermsSync = false
                    }
                }
            }
        }
    }

    // ============================================================
    // Conversion Helpers
    // ============================================================

    /**
     * Converts a LuckPerms Group to a Brennon RankData.
     */
    private fun groupToRankData(group: Group, rankId: String): RankData? {
        val permissions = mutableSetOf<String>()
        val inheritance = mutableSetOf<String>()
        var prefix = ""
        var suffix = ""
        var weight = 0

        for (node in group.getNodes()) {
            when (node) {
                is PermissionNode -> {
                    if (node.value) {
                        permissions.add(node.permission)
                    } else {
                        permissions.add("-${node.permission}")
                    }
                }
                is InheritanceNode -> {
                    inheritance.add(fromLuckPermsGroupName(node.groupName))
                }
                is PrefixNode -> {
                    prefix = node.metaValue
                }
                is SuffixNode -> {
                    suffix = node.metaValue
                }
                is WeightNode -> {
                    weight = node.weight
                }
            }
        }

        // Check if existing rank has metadata we should preserve
        val existing = rankManager.getCoreRank(rankId)?.getData()

        return RankData(
            id = rankId,
            displayName = existing?.displayName ?: rankId.replaceFirstChar { it.uppercase() },
            prefix = prefix.ifBlank { existing?.prefix ?: "" },
            suffix = suffix.ifBlank { existing?.suffix ?: "" },
            weight = weight,
            permissions = permissions,
            inheritance = inheritance,
            isDefault = existing?.isDefault ?: (rankId == "default"),
            isStaff = existing?.isStaff ?: false,
            metadata = existing?.metadata ?: mutableMapOf()
        )
    }

    /**
     * Converts a Brennon rank ID to a LuckPerms group name.
     * Applies the configured group prefix.
     */
    fun toLuckPermsGroupName(rankId: String): String {
        return "${config.groupPrefix}$rankId"
    }

    /**
     * Converts a LuckPerms group name to a Brennon rank ID.
     * Strips the configured group prefix.
     */
    fun fromLuckPermsGroupName(groupName: String): String {
        return if (config.groupPrefix.isNotEmpty() && groupName.startsWith(config.groupPrefix)) {
            groupName.removePrefix(config.groupPrefix)
        } else {
            groupName
        }
    }

    // ============================================================
    // Pending Operations (Loop Prevention)
    // ============================================================

    private fun addPendingOp(key: String) {
        pendingOps[key] = System.currentTimeMillis()
    }

    private fun consumePendingOp(key: String): Boolean {
        val timestamp = pendingOps.remove(key) ?: return false
        // Expire after 5 seconds
        return System.currentTimeMillis() - timestamp < 5_000
    }

    /**
     * Cleans up expired pending operation keys.
     * Called periodically by the scheduler.
     */
    fun cleanupPendingOps() {
        val now = System.currentTimeMillis()
        pendingOps.entries.removeIf { now - it.value > 5_000 }
    }
}
