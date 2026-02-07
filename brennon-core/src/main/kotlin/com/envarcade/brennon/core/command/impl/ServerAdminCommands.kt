package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.model.ServerDefinition
import com.envarcade.brennon.common.model.ServerGroupDefinition
import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender

/**
 * /serveradmin (alias: /sa) — Manage server registry
 */
class ServerAdminCommand(private val brennon: Brennon) : BrennonCommand(
    name = "serveradmin",
    permission = "brennon.admin.server",
    aliases = listOf("sa"),
    usage = "/sa <add|remove|list|info|setgroup>",
    description = "Manage the dynamic server registry"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
            return
        }

        when (args[0].lowercase()) {
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "list" -> handleList(sender, args)
            "info" -> handleInfo(sender, args)
            "setgroup" -> handleSetGroup(sender, args)
            else -> sendUsage(sender)
        }
    }

    private fun handleAdd(sender: BrennonCommandSender, args: Array<String>) {
        // /sa add <name> <group> <host:port>
        if (args.size < 4) {
            sender.sendMessage(TextUtil.error("Usage: /sa add <name> <group> <host:port>"))
            return
        }

        val name = args[1]
        val group = args[2]
        val addressParts = args[3].split(":")
        if (addressParts.size != 2) {
            sender.sendMessage(TextUtil.error("Invalid address format. Use <host>:<port>"))
            return
        }

        val host = addressParts[0]
        val port = addressParts[1].toIntOrNull()
        if (port == null || port < 1 || port > 65535) {
            sender.sendMessage(TextUtil.error("Invalid port number."))
            return
        }

        val def = ServerDefinition(
            name = name,
            group = group,
            host = host,
            port = port,
            addedBy = sender.name
        )

        if (brennon.serverRegistryService.registerServer(def)) {
            sender.sendMessage(TextUtil.success("Server <white>$name</white> registered at <white>$host:$port</white> in group <white>$group</white>."))
        } else {
            sender.sendMessage(TextUtil.error("A server named '$name' already exists."))
        }
    }

    private fun handleRemove(sender: BrennonCommandSender, args: Array<String>) {
        // /sa remove <name>
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: /sa remove <name>"))
            return
        }

        val name = args[1]
        if (brennon.serverRegistryService.unregisterServer(name)) {
            sender.sendMessage(TextUtil.success("Server <white>$name</white> unregistered."))
        } else {
            sender.sendMessage(TextUtil.error("Server '$name' not found in registry."))
        }
    }

    private fun handleList(sender: BrennonCommandSender, args: Array<String>) {
        // /sa list [group]
        val filterGroup = if (args.size > 1) args[1] else null
        val servers = if (filterGroup != null) {
            brennon.serverRegistryService.getServerDefinitionsByGroup(filterGroup)
        } else {
            brennon.serverRegistryService.getAllServerDefinitions()
        }

        if (servers.isEmpty()) {
            sender.sendMessage(TextUtil.prefixed(
                if (filterGroup != null) "No servers in group '$filterGroup'."
                else "No servers registered."
            ))
            return
        }

        val title = if (filterGroup != null) "Servers in group <yellow>$filterGroup</yellow>" else "All registered servers"
        sender.sendMessage(TextUtil.prefixed("$title (${servers.size}):"))

        val grouped = servers.groupBy { it.group }
        for ((group, groupServers) in grouped) {
            sender.sendMessage(TextUtil.parse("  <yellow>$group</yellow>:"))
            for (srv in groupServers) {
                val liveInfo = brennon.coreServerManager.getServer(srv.name).orElse(null)
                val status = when {
                    liveInfo?.isOnline == true -> "<green>ONLINE</green>"
                    else -> "<red>OFFLINE</red>"
                }
                val auto = if (srv.autoRegistered) " <dark_gray>(auto)" else ""
                sender.sendMessage(TextUtil.parse(
                    "    <gray>- <white>${srv.name}</white> <gray>${srv.address} [$status<gray>]$auto"
                ))
            }
        }
    }

    private fun handleInfo(sender: BrennonCommandSender, args: Array<String>) {
        // /sa info <name>
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: /sa info <name>"))
            return
        }

        val name = args[1]
        val def = brennon.serverRegistryService.getServerDefinition(name)
        if (def == null) {
            sender.sendMessage(TextUtil.error("Server '$name' not found in registry."))
            return
        }

        val liveInfo = brennon.coreServerManager.getServer(name).orElse(null)
        val status = if (liveInfo?.isOnline == true) "<green>ONLINE</green>" else "<red>OFFLINE</red>"
        val players = liveInfo?.playerCount ?: 0

        sender.sendMessage(TextUtil.prefixed("Server info: <white>${def.name}</white>"))
        sender.sendMessage(TextUtil.parse("  <gray>Group: <white>${def.group}"))
        sender.sendMessage(TextUtil.parse("  <gray>Address: <white>${def.address}"))
        sender.sendMessage(TextUtil.parse("  <gray>Status: $status"))
        sender.sendMessage(TextUtil.parse("  <gray>Players: <white>$players/${def.maxPlayers}"))
        sender.sendMessage(TextUtil.parse("  <gray>Restricted: <white>${def.restricted}"))
        if (def.restricted) {
            sender.sendMessage(TextUtil.parse("  <gray>Permission: <white>${def.permission}"))
        }
        sender.sendMessage(TextUtil.parse("  <gray>Auto-registered: <white>${def.autoRegistered}"))
        sender.sendMessage(TextUtil.parse("  <gray>Added by: <white>${def.addedBy}"))
    }

    private fun handleSetGroup(sender: BrennonCommandSender, args: Array<String>) {
        // /sa setgroup <name> <group>
        if (args.size < 3) {
            sender.sendMessage(TextUtil.error("Usage: /sa setgroup <name> <group>"))
            return
        }

        val name = args[1]
        val newGroup = args[2]

        if (brennon.serverRegistryService.updateServerGroup(name, newGroup)) {
            sender.sendMessage(TextUtil.success("Server <white>$name</white> moved to group <white>$newGroup</white>."))
        } else {
            sender.sendMessage(TextUtil.error("Server '$name' not found in registry."))
        }
    }

    private fun sendUsage(sender: BrennonCommandSender) {
        sender.sendMessage(TextUtil.prefixed("Server Admin Commands:"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sa add <name> <group> <host:port></yellow> <gray>- Register a server"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sa remove <name></yellow> <gray>- Unregister a server"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sa list [group]</yellow> <gray>- List registered servers"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sa info <name></yellow> <gray>- Show server details"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sa setgroup <name> <group></yellow> <gray>- Move server to group"))
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("add", "remove", "list", "info", "setgroup")
                .filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) {
                "remove", "info", "setgroup" ->
                    brennon.serverRegistryService.getAllServerDefinitions().map { it.name }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                "list" ->
                    brennon.serverRegistryService.getAllGroupDefinitions().map { it.id }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "add", "setgroup" ->
                    brennon.serverRegistryService.getAllGroupDefinitions().map { it.id }
                        .filter { it.startsWith(args[2], ignoreCase = true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}

/**
 * /servergroup (alias: /sg) — Manage server groups
 */
class ServerGroupCommand(private val brennon: Brennon) : BrennonCommand(
    name = "servergroup",
    permission = "brennon.admin.servergroup",
    aliases = listOf("sg"),
    usage = "/sg <create|delete|list|info|set>",
    description = "Manage server groups"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
            return
        }

        when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args)
            "delete" -> handleDelete(sender, args)
            "list" -> handleList(sender)
            "info" -> handleInfo(sender, args)
            "set" -> handleSet(sender, args)
            else -> sendUsage(sender)
        }
    }

    private fun handleCreate(sender: BrennonCommandSender, args: Array<String>) {
        // /sg create <id> [displayName...]
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: /sg create <id> [displayName]"))
            return
        }

        val id = args[1]
        val displayName = if (args.size > 2) args.drop(2).joinToString(" ") else id

        val group = ServerGroupDefinition(id = id, displayName = displayName)
        if (brennon.serverRegistryService.createGroup(group)) {
            sender.sendMessage(TextUtil.success("Group <white>$id</white> created (display: <white>$displayName</white>)."))
        } else {
            sender.sendMessage(TextUtil.error("Group '$id' already exists."))
        }
    }

    private fun handleDelete(sender: BrennonCommandSender, args: Array<String>) {
        // /sg delete <id>
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: /sg delete <id>"))
            return
        }

        val id = args[1]
        if (brennon.serverRegistryService.deleteGroup(id)) {
            sender.sendMessage(TextUtil.success("Group <white>$id</white> deleted."))
        } else {
            val group = brennon.serverRegistryService.getGroupDefinition(id)
            if (group == null) {
                sender.sendMessage(TextUtil.error("Group '$id' not found."))
            } else {
                val count = brennon.serverRegistryService.getServerDefinitionsByGroup(id).size
                sender.sendMessage(TextUtil.error("Cannot delete group '$id': $count server(s) still assigned. Move or remove them first."))
            }
        }
    }

    private fun handleList(sender: BrennonCommandSender) {
        val groups = brennon.serverRegistryService.getAllGroupDefinitions()
        if (groups.isEmpty()) {
            sender.sendMessage(TextUtil.prefixed("No server groups defined."))
            return
        }

        sender.sendMessage(TextUtil.prefixed("Server Groups (${groups.size}):"))
        for (group in groups.sortedByDescending { it.joinPriority }) {
            val serverCount = brennon.serverRegistryService.getServerDefinitionsByGroup(group.id).size
            val flags = buildList {
                if (group.isFallback) add("<green>fallback</green>")
                if (group.restricted) add("<red>restricted</red>")
            }.joinToString(" ")
            val flagStr = if (flags.isNotEmpty()) " $flags" else ""
            sender.sendMessage(TextUtil.parse(
                "  <yellow>${group.id}</yellow> <gray>(${group.displayName}) <white>$serverCount server(s)$flagStr"
            ))
        }
    }

    private fun handleInfo(sender: BrennonCommandSender, args: Array<String>) {
        // /sg info <id>
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: /sg info <id>"))
            return
        }

        val id = args[1]
        val group = brennon.serverRegistryService.getGroupDefinition(id)
        if (group == null) {
            sender.sendMessage(TextUtil.error("Group '$id' not found."))
            return
        }

        val servers = brennon.serverRegistryService.getServerDefinitionsByGroup(id)
        sender.sendMessage(TextUtil.prefixed("Group info: <white>${group.id}</white>"))
        sender.sendMessage(TextUtil.parse("  <gray>Display Name: <white>${group.displayName}"))
        sender.sendMessage(TextUtil.parse("  <gray>Join Priority: <white>${group.joinPriority}"))
        sender.sendMessage(TextUtil.parse("  <gray>Fallback: <white>${group.isFallback}"))
        sender.sendMessage(TextUtil.parse("  <gray>Restricted: <white>${group.restricted}"))
        if (group.restricted) {
            sender.sendMessage(TextUtil.parse("  <gray>Permission: <white>${group.permission}"))
        }
        sender.sendMessage(TextUtil.parse("  <gray>Max Players: <white>${if (group.maxPlayers < 0) "unlimited" else group.maxPlayers.toString()}"))
        sender.sendMessage(TextUtil.parse("  <gray>Servers (${servers.size}):"))
        for (srv in servers) {
            sender.sendMessage(TextUtil.parse("    <gray>- <white>${srv.name} <dark_gray>(${srv.address})"))
        }
    }

    private fun handleSet(sender: BrennonCommandSender, args: Array<String>) {
        // /sg set <id> <property> <value>
        if (args.size < 4) {
            sender.sendMessage(TextUtil.error("Usage: /sg set <id> <property> <value>"))
            sender.sendMessage(TextUtil.parse("  <gray>Properties: displayName, joinPriority, restricted, permission, isFallback"))
            return
        }

        val id = args[1]
        val property = args[2].lowercase()
        val value = args.drop(3).joinToString(" ")

        val group = brennon.serverRegistryService.getGroupDefinition(id)
        if (group == null) {
            sender.sendMessage(TextUtil.error("Group '$id' not found."))
            return
        }

        val updated = when (property) {
            "displayname" -> group.copy(displayName = value)
            "joinpriority" -> {
                val priority = value.toIntOrNull()
                if (priority == null) {
                    sender.sendMessage(TextUtil.error("joinPriority must be a number."))
                    return
                }
                group.copy(joinPriority = priority)
            }
            "restricted" -> group.copy(restricted = value.toBooleanStrictOrNull() ?: false)
            "permission" -> group.copy(permission = value)
            "isfallback" -> group.copy(isFallback = value.toBooleanStrictOrNull() ?: false)
            "maxplayers" -> {
                val max = value.toIntOrNull()
                if (max == null) {
                    sender.sendMessage(TextUtil.error("maxPlayers must be a number (-1 for unlimited)."))
                    return
                }
                group.copy(maxPlayers = max)
            }
            else -> {
                sender.sendMessage(TextUtil.error("Unknown property: $property"))
                sender.sendMessage(TextUtil.parse("  <gray>Properties: displayName, joinPriority, restricted, permission, isFallback, maxPlayers"))
                return
            }
        }

        brennon.serverRegistryService.updateGroup(updated)
        sender.sendMessage(TextUtil.success("Group <white>$id</white>: set <white>$property</white> to <white>$value</white>."))
    }

    private fun sendUsage(sender: BrennonCommandSender) {
        sender.sendMessage(TextUtil.prefixed("Server Group Commands:"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sg create <id> [displayName]</yellow> <gray>- Create a group"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sg delete <id></yellow> <gray>- Delete a group"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sg list</yellow> <gray>- List all groups"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sg info <id></yellow> <gray>- Show group details"))
        sender.sendMessage(TextUtil.parse("  <yellow>/sg set <id> <property> <value></yellow> <gray>- Set group property"))
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("create", "delete", "list", "info", "set")
                .filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) {
                "delete", "info", "set" ->
                    brennon.serverRegistryService.getAllGroupDefinitions().map { it.id }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "set" -> listOf("displayName", "joinPriority", "restricted", "permission", "isFallback", "maxPlayers")
                    .filter { it.startsWith(args[2], ignoreCase = true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
