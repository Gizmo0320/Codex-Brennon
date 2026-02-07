package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.common.util.TimeUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender

/**
 * /ban <player> [duration] <reason>
 * Bans a player from the network.
 */
class BanCommand(private val brennon: Brennon) : BrennonCommand(
    name = "ban",
    permission = "brennon.command.ban",
    aliases = listOf("networkban", "nban"),
    usage = "/ban <player> [duration] <reason>",
    description = "Ban a player from the network"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            sender.sendMessage(TextUtil.error("Duration examples: 1d, 12h, 30m, perm"))
            return
        }

        val targetName = args[0]
        val duration = TimeUtil.parseDuration(args[1])
        val reasonStart = if (duration != null || args[1].equals("perm", true)) 2 else 1
        val reason = if (args.size > reasonStart) args.drop(reasonStart).joinToString(" ") else "No reason specified"

        brennon.corePlayerManager.getPlayer(targetName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$targetName' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.corePunishmentManager.ban(target.uniqueId, reason, duration, sender.uuid).thenAccept { punishment ->
                val durationStr = TimeUtil.formatDuration(duration)
                sender.sendMessage(TextUtil.success(
                    "Banned <white>${target.name}</white> for <white>$durationStr</white>: <white>$reason</white>"
                ))
            }.exceptionally { error ->
                sender.sendMessage(TextUtil.error("Failed to ban: ${error.message}"))
                null
            }
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> brennon.corePlayerManager.getOnlinePlayers().map { it.getName() }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
            2 -> listOf("1h", "1d", "7d", "30d", "perm")
                .filter { it.startsWith(args[1].lowercase()) }
            else -> emptyList()
        }
    }
}

/**
 * /unban <player>
 */
class UnbanCommand(private val brennon: Brennon) : BrennonCommand(
    name = "unban",
    permission = "brennon.command.unban",
    aliases = listOf("pardon"),
    usage = "/unban <player>",
    description = "Unban a player"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        brennon.corePlayerManager.getPlayer(args[0]).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '${args[0]}' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.corePunishmentManager.unban(target.uniqueId, sender.uuid).thenRun {
                sender.sendMessage(TextUtil.success("Unbanned <white>${target.name}</white>."))
            }.exceptionally { error ->
                sender.sendMessage(TextUtil.error("Failed to unban: ${error.message}"))
                null
            }
        }
    }
}

/**
 * /mute <player> [duration] <reason>
 */
class MuteCommand(private val brennon: Brennon) : BrennonCommand(
    name = "mute",
    permission = "brennon.command.mute",
    aliases = listOf("networkmute"),
    usage = "/mute <player> [duration] <reason>",
    description = "Mute a player on the network"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        val targetName = args[0]
        val duration = TimeUtil.parseDuration(args[1])
        val reasonStart = if (duration != null || args[1].equals("perm", true)) 2 else 1
        val reason = if (args.size > reasonStart) args.drop(reasonStart).joinToString(" ") else "No reason specified"

        brennon.corePlayerManager.getPlayer(targetName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$targetName' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.corePunishmentManager.mute(target.uniqueId, reason, duration, sender.uuid).thenAccept { _ ->
                val durationStr = TimeUtil.formatDuration(duration)
                sender.sendMessage(TextUtil.success(
                    "Muted <white>${target.name}</white> for <white>$durationStr</white>: <white>$reason</white>"
                ))
            }.exceptionally { error ->
                sender.sendMessage(TextUtil.error("Failed to mute: ${error.message}"))
                null
            }
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> brennon.corePlayerManager.getOnlinePlayers().map { it.getName() }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
            2 -> listOf("1h", "1d", "7d", "30d", "perm")
                .filter { it.startsWith(args[1].lowercase()) }
            else -> emptyList()
        }
    }
}

/**
 * /unmute <player>
 */
class UnmuteCommand(private val brennon: Brennon) : BrennonCommand(
    name = "unmute",
    permission = "brennon.command.unmute",
    usage = "/unmute <player>",
    description = "Unmute a player"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        brennon.corePlayerManager.getPlayer(args[0]).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '${args[0]}' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.corePunishmentManager.unmute(target.uniqueId, sender.uuid).thenRun {
                sender.sendMessage(TextUtil.success("Unmuted <white>${target.name}</white>."))
            }
        }
    }
}

/**
 * /kick <player> [reason]
 */
class KickCommand(private val brennon: Brennon) : BrennonCommand(
    name = "kick",
    permission = "brennon.command.kick",
    usage = "/kick <player> [reason]",
    description = "Kick a player from the network"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        val targetName = args[0]
        val reason = if (args.size > 1) args.drop(1).joinToString(" ") else "Kicked by staff"

        brennon.corePlayerManager.getPlayer(targetName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$targetName' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.corePunishmentManager.kick(target.uniqueId, reason, sender.uuid).thenAccept { _ ->
                sender.sendMessage(TextUtil.success("Kicked <white>${target.name}</white>: <white>$reason</white>"))
            }
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return brennon.corePlayerManager.getOnlinePlayers().map { it.getName() }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}

/**
 * /warn <player> <reason>
 */
class WarnCommand(private val brennon: Brennon) : BrennonCommand(
    name = "warn",
    permission = "brennon.command.warn",
    usage = "/warn <player> <reason>",
    description = "Warn a player"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        val targetName = args[0]
        val reason = args.drop(1).joinToString(" ")

        brennon.corePlayerManager.getPlayer(targetName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$targetName' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.corePunishmentManager.warn(target.uniqueId, reason, sender.uuid).thenAccept { _ ->
                sender.sendMessage(TextUtil.success("Warned <white>${target.name}</white>: <white>$reason</white>"))

                // Also message the target if online
                if (target.isOnline) {
                    target.sendMessage(TextUtil.error("You have been warned: <white>$reason</white>"))
                }
            }
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return brennon.corePlayerManager.getOnlinePlayers().map { it.getName() }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}

/**
 * /history <player> [--all-networks] — View punishment history
 */
class HistoryCommand(private val brennon: Brennon) : BrennonCommand(
    name = "history",
    permission = "brennon.command.history",
    aliases = listOf("punishmenthistory", "ph"),
    usage = "/history <player> [--all-networks]",
    description = "View a player's punishment history"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        val allNetworks = args.any { it.equals("--all-networks", ignoreCase = true) }
        val targetName = args.first { !it.startsWith("--") }

        if (allNetworks && !sender.hasPermission("brennon.admin.crossnetwork")) {
            sender.sendMessage(TextUtil.error("You don't have permission to view cross-network history."))
            return
        }

        brennon.corePlayerManager.getPlayer(targetName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$targetName' not found."))
                return@thenAccept
            }

            val target = opt.get()

            if (allNetworks) {
                brennon.crossNetworkService.getAllPunishments(target.uniqueId).thenAccept { punishments ->
                    if (punishments.isEmpty()) {
                        sender.sendMessage(TextUtil.prefixed("<white>${target.name}</white> has no punishment history across all networks."))
                        return@thenAccept
                    }

                    sender.sendMessage(TextUtil.prefixed("Punishment history for <white>${target.name}</white> across all networks (${punishments.size} total):"))
                    for (p in punishments.take(10)) {
                        val status = if (p.active) "<red>ACTIVE</red>" else "<green>EXPIRED</green>"
                        val time = TimeUtil.format(p.issuedAt)
                        val network = p.networkId ?: "global"
                        sender.sendMessage(TextUtil.parse(
                            "  <gray>[$status<gray>] <aqua>[$network]</aqua> <yellow>${p.type.name}</yellow> - ${p.reason} <dark_gray>($time) [${p.id}]"
                        ))
                    }
                    if (punishments.size > 10) {
                        sender.sendMessage(TextUtil.parse("  <gray>... and ${punishments.size - 10} more"))
                    }
                }
            } else {
                brennon.corePunishmentManager.getHistory(target.uniqueId).thenAccept { history ->
                    if (history.isEmpty()) {
                        sender.sendMessage(TextUtil.prefixed("<white>${target.name}</white> has no punishment history."))
                        return@thenAccept
                    }

                    sender.sendMessage(TextUtil.prefixed("Punishment history for <white>${target.name}</white> (${history.size} total):"))
                    for (p in history.take(10)) {
                        val status = if (p.isActive) "<red>ACTIVE</red>" else "<green>EXPIRED</green>"
                        val time = TimeUtil.format(p.issuedAt)
                        sender.sendMessage(TextUtil.parse(
                            "  <gray>[$status<gray>] <yellow>${p.type.name}</yellow> - ${p.reason} <dark_gray>($time) [${p.id}]"
                        ))
                    }
                    if (history.size > 10) {
                        sender.sendMessage(TextUtil.parse("  <gray>... and ${history.size - 10} more"))
                    }
                }
            }
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> brennon.corePlayerManager.getOnlinePlayers().map { it.getName() }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
            2 -> if (sender.hasPermission("brennon.admin.crossnetwork"))
                listOf("--all-networks").filter { it.startsWith(args[1], ignoreCase = true) }
            else emptyList()
            else -> emptyList()
        }
    }
}
