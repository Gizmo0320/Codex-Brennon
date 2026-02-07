package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender

/**
 * /rank <set|add|remove|list|info> ...
 * Manage player ranks and permissions.
 */
class RankCommand(private val brennon: Brennon) : BrennonCommand(
    name = "rank",
    permission = "brennon.command.rank",
    aliases = listOf("ranks", "setrank"),
    usage = "/rank <set|add|remove|list|info> ...",
    description = "Manage player ranks"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sendHelp(sender)
            return
        }

        when (args[0].lowercase()) {
            "set" -> handleSet(sender, args)
            "add" -> handleAdd(sender, args)
            "remove" -> handleRemove(sender, args)
            "list" -> handleList(sender)
            "info" -> handleInfo(sender, args)
            else -> sendHelp(sender)
        }
    }

    private fun handleSet(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(TextUtil.error("Usage: /rank set <player> <rank>"))
            return
        }

        val playerName = args[1]
        val rankId = args[2]

        if (brennon.coreRankManager.getRank(rankId).isEmpty) {
            sender.sendMessage(TextUtil.error("Rank '$rankId' does not exist."))
            return
        }

        brennon.corePlayerManager.getPlayer(playerName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$playerName' not found."))
                return@thenAccept
            }

            brennon.coreRankManager.setPlayerRank(opt.get().uniqueId, rankId).thenRun {
                sender.sendMessage(TextUtil.success(
                    "Set <white>$playerName</white>'s rank to <white>$rankId</white>."
                ))
            }.exceptionally { error ->
                sender.sendMessage(TextUtil.error("Failed: ${error.message}"))
                null
            }
        }
    }

    private fun handleAdd(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(TextUtil.error("Usage: /rank add <player> <rank>"))
            return
        }

        val playerName = args[1]
        val rankId = args[2]

        brennon.corePlayerManager.getPlayer(playerName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$playerName' not found."))
                return@thenAccept
            }

            brennon.coreRankManager.addPlayerRank(opt.get().uniqueId, rankId).thenRun {
                sender.sendMessage(TextUtil.success(
                    "Added rank <white>$rankId</white> to <white>$playerName</white>."
                ))
            }
        }
    }

    private fun handleRemove(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(TextUtil.error("Usage: /rank remove <player> <rank>"))
            return
        }

        val playerName = args[1]
        val rankId = args[2]

        brennon.corePlayerManager.getPlayer(playerName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$playerName' not found."))
                return@thenAccept
            }

            brennon.coreRankManager.removePlayerRank(opt.get().uniqueId, rankId).thenRun {
                sender.sendMessage(TextUtil.success(
                    "Removed rank <white>$rankId</white> from <white>$playerName</white>."
                ))
            }
        }
    }

    private fun handleList(sender: BrennonCommandSender) {
        val ranks = brennon.coreRankManager.getRanks().sortedByDescending { it.weight }
        sender.sendMessage(TextUtil.prefixed("Ranks (${ranks.size}):"))
        for (rank in ranks) {
            val prefix = rank.prefix
            val defaultTag = if (rank.isDefault) " <yellow>(default)</yellow>" else ""
            val staffTag = if (rank.isStaff) " <red>(staff)</red>" else ""
            sender.sendMessage(TextUtil.parse(
                "  <gray>- </gray>${TextUtil.serialize(prefix)} <white>${rank.id}</white> <dark_gray>[w:${rank.weight}]</dark_gray>$defaultTag$staffTag"
            ))
        }
    }

    private fun handleInfo(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: /rank info <rank>"))
            return
        }

        val rank = brennon.coreRankManager.getRank(args[1]).orElse(null)
        if (rank == null) {
            sender.sendMessage(TextUtil.error("Rank '${args[1]}' does not exist."))
            return
        }

        sender.sendMessage(TextUtil.prefixed("Rank info: <white>${rank.id}</white>"))
        sender.sendMessage(TextUtil.parse("  <gray>Display: <white>${rank.displayName}</white>"))
        sender.sendMessage(TextUtil.parse("  <gray>Prefix: ${TextUtil.serialize(rank.prefix)}"))
        sender.sendMessage(TextUtil.parse("  <gray>Weight: <white>${rank.weight}</white>"))
        sender.sendMessage(TextUtil.parse("  <gray>Default: <white>${rank.isDefault}</white>"))
        sender.sendMessage(TextUtil.parse("  <gray>Staff: <white>${rank.isStaff}</white>"))
        sender.sendMessage(TextUtil.parse("  <gray>Permissions (${rank.permissions.size}): <white>${rank.permissions.take(5).joinToString(", ")}${if (rank.permissions.size > 5) "..." else ""}</white>"))
        sender.sendMessage(TextUtil.parse("  <gray>Inherits: <white>${rank.inheritance.joinToString(", ").ifEmpty { "none" }}</white>"))
    }

    private fun sendHelp(sender: BrennonCommandSender) {
        sender.sendMessage(TextUtil.prefixed("Rank Commands:"))
        sender.sendMessage(TextUtil.parse("  <yellow>/rank set <player> <rank></yellow> <gray>— Set primary rank"))
        sender.sendMessage(TextUtil.parse("  <yellow>/rank add <player> <rank></yellow> <gray>— Add secondary rank"))
        sender.sendMessage(TextUtil.parse("  <yellow>/rank remove <player> <rank></yellow> <gray>— Remove a rank"))
        sender.sendMessage(TextUtil.parse("  <yellow>/rank list</yellow> <gray>— List all ranks"))
        sender.sendMessage(TextUtil.parse("  <yellow>/rank info <rank></yellow> <gray>— View rank details"))
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("set", "add", "remove", "list", "info")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "set", "add", "remove" -> brennon.corePlayerManager.getOnlinePlayers().map { it.getName() }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
                "info" -> brennon.coreRankManager.getRanks().map { it.id }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "set", "add", "remove" -> brennon.coreRankManager.getRanks().map { it.id }
                    .filter { it.lowercase().startsWith(args[2].lowercase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
