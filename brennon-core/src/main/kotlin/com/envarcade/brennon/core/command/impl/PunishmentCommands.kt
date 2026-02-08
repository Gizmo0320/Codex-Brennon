package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.common.util.TimeUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender
import com.envarcade.brennon.core.player.CoreNetworkPlayer
import java.time.Duration
import java.time.Instant

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

/**
 * /ipban <player> [duration] <reason>
 * IP bans a player (and all accounts on that IP).
 */
class IpBanCommand(private val brennon: Brennon) : BrennonCommand(
    name = "ipban",
    permission = "brennon.command.ipban",
    aliases = listOf("banip"),
    usage = "/ipban <player> [duration] <reason>",
    description = "IP ban a player from the network"
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
            val ip = (target as? CoreNetworkPlayer)?.getData()?.ipAddress
            if (ip.isNullOrEmpty() || ip == "unknown") {
                sender.sendMessage(TextUtil.error("No IP address on record for '${target.name}'."))
                return@thenAccept
            }

            brennon.corePunishmentManager.ipBan(target.uniqueId, ip, reason, duration, sender.uuid).thenAccept { punishment ->
                val durationStr = TimeUtil.formatDuration(duration)
                sender.sendMessage(TextUtil.success(
                    "IP banned <white>${target.name}</white> (<white>$ip</white>) for <white>$durationStr</white>: <white>$reason</white>"
                ))
            }.exceptionally { error ->
                sender.sendMessage(TextUtil.error("Failed to IP ban: ${error.message}"))
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
 * /unipban <player|ip>
 * Removes an IP ban.
 */
class UnIpBanCommand(private val brennon: Brennon) : BrennonCommand(
    name = "unipban",
    permission = "brennon.command.unipban",
    aliases = listOf("unbanip"),
    usage = "/unipban <player|ip>",
    description = "Remove an IP ban"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        val input = args[0]
        // Check if input looks like an IP address
        val isIp = input.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))

        if (isIp) {
            brennon.corePunishmentManager.unIpBan(input, sender.uuid).thenRun {
                sender.sendMessage(TextUtil.success("Removed IP ban for <white>$input</white>."))
            }.exceptionally { error ->
                sender.sendMessage(TextUtil.error("Failed to remove IP ban: ${error.message}"))
                null
            }
        } else {
            brennon.corePlayerManager.getPlayer(input).thenAccept { opt ->
                if (opt.isEmpty) {
                    sender.sendMessage(TextUtil.error("Player '$input' not found."))
                    return@thenAccept
                }

                val target = opt.get()
                val ip = (target as? CoreNetworkPlayer)?.getData()?.ipAddress
                if (ip.isNullOrEmpty() || ip == "unknown") {
                    sender.sendMessage(TextUtil.error("No IP address on record for '${target.name}'."))
                    return@thenAccept
                }

                brennon.corePunishmentManager.unIpBan(ip, sender.uuid).thenRun {
                    sender.sendMessage(TextUtil.success(
                        "Removed IP ban for <white>${target.name}</white> (<white>$ip</white>)."
                    ))
                }.exceptionally { error ->
                    sender.sendMessage(TextUtil.error("Failed to remove IP ban: ${error.message}"))
                    null
                }
            }
        }
    }
}

/**
 * /lookup <id> — View full punishment details by ID.
 */
class LookupCommand(private val brennon: Brennon) : BrennonCommand(
    name = "lookup",
    permission = "brennon.command.lookup",
    aliases = listOf("plookup", "punishmentlookup"),
    usage = "/lookup <punishment-id>",
    description = "Look up a punishment by ID"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        val id = args[0]
        brennon.databaseManager.punishments.findById(id).thenAccept { data ->
            if (data == null) {
                sender.sendMessage(TextUtil.error("No punishment found with ID: $id"))
                return@thenAccept
            }

            // Resolve player names
            val targetName = brennon.corePlayerManager.getCachedPlayer(data.target)?.getName() ?: data.target.toString()
            val issuerName = data.issuer?.let {
                brennon.corePlayerManager.getCachedPlayer(it)?.getName() ?: it.toString()
            } ?: "CONSOLE"

            sender.sendMessage(TextUtil.prefixed("Punishment Lookup: <white>${data.id}</white>"))
            sender.sendMessage(TextUtil.parse("  <gray>Type: <yellow>${data.type.name}"))
            sender.sendMessage(TextUtil.parse("  <gray>Target: <white>$targetName</white> <dark_gray>(${data.target})"))
            sender.sendMessage(TextUtil.parse("  <gray>Issuer: <white>$issuerName"))
            sender.sendMessage(TextUtil.parse("  <gray>Reason: <white>${data.reason}"))
            sender.sendMessage(TextUtil.parse("  <gray>Issued: <white>${TimeUtil.format(data.issuedAt)}"))

            if (data.expiresAt != null) {
                sender.sendMessage(TextUtil.parse("  <gray>Expires: <white>${TimeUtil.format(data.expiresAt!!)}"))
            } else {
                sender.sendMessage(TextUtil.parse("  <gray>Duration: <red>Permanent"))
            }

            val status = when {
                data.revokedBy != null -> "<green>REVOKED"
                !data.isEffectivelyActive() -> "<gray>EXPIRED"
                else -> "<red>ACTIVE"
            }
            sender.sendMessage(TextUtil.parse("  <gray>Status: $status"))

            if (data.revokedBy != null) {
                val revokerName = data.revokedBy?.let {
                    brennon.corePlayerManager.getCachedPlayer(it)?.getName() ?: it.toString()
                } ?: "CONSOLE"
                sender.sendMessage(TextUtil.parse("  <gray>Revoked by: <white>$revokerName"))
                if (data.revokeReason != null) {
                    sender.sendMessage(TextUtil.parse("  <gray>Revoke reason: <white>${data.revokeReason}"))
                }
            }

            if (data.targetIp != null) {
                sender.sendMessage(TextUtil.parse("  <gray>Target IP: <white>${data.targetIp}"))
            }

            if (data.networkId != null) {
                sender.sendMessage(TextUtil.parse("  <gray>Network: <white>${data.networkId}"))
            }
        }.exceptionally { error ->
            sender.sendMessage(TextUtil.error("Failed to look up punishment: ${error.message}"))
            null
        }
    }
}

/**
 * /editpunishment <id> <reason|duration|revoke> <value>
 * Edit a punishment's reason, duration, or revoke it.
 */
class EditPunishmentCommand(private val brennon: Brennon) : BrennonCommand(
    name = "editpunishment",
    permission = "brennon.command.editpunishment",
    aliases = listOf("editpun", "pedit"),
    usage = "/editpunishment <id> <reason|duration|revoke> [value]",
    description = "Edit or revoke a punishment"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            sender.sendMessage(TextUtil.error("Actions: reason <new reason>, duration <new duration>, revoke [reason]"))
            return
        }

        val id = args[0]
        val action = args[1].lowercase()

        brennon.databaseManager.punishments.findById(id).thenAccept { data ->
            if (data == null) {
                sender.sendMessage(TextUtil.error("No punishment found with ID: $id"))
                return@thenAccept
            }

            when (action) {
                "reason" -> {
                    if (args.size < 3) {
                        sender.sendMessage(TextUtil.error("Usage: /editpunishment $id reason <new reason>"))
                        return@thenAccept
                    }
                    val newReason = args.drop(2).joinToString(" ")
                    val updated = data.copy(reason = newReason)
                    brennon.databaseManager.punishments.save(updated).thenRun {
                        sender.sendMessage(TextUtil.success("Updated reason for <white>$id</white> to: <white>$newReason</white>"))
                    }
                }
                "duration" -> {
                    if (args.size < 3) {
                        sender.sendMessage(TextUtil.error("Usage: /editpunishment $id duration <new duration>"))
                        return@thenAccept
                    }
                    val newDuration = TimeUtil.parseDuration(args[2])
                    if (newDuration == null && !args[2].equals("perm", true)) {
                        sender.sendMessage(TextUtil.error("Invalid duration: ${args[2]}. Examples: 1h, 1d, 7d, perm"))
                        return@thenAccept
                    }
                    val newExpiresAt = newDuration?.let { data.issuedAt.plus(it) }
                    val updated = data.copy(expiresAt = newExpiresAt)
                    brennon.databaseManager.punishments.save(updated).thenRun {
                        val durationStr = TimeUtil.formatDuration(newDuration)
                        sender.sendMessage(TextUtil.success("Updated duration for <white>$id</white> to: <white>$durationStr</white>"))
                    }
                }
                "revoke" -> {
                    if (!data.isEffectivelyActive()) {
                        sender.sendMessage(TextUtil.error("Punishment $id is not active."))
                        return@thenAccept
                    }
                    val revokeReason = if (args.size > 2) args.drop(2).joinToString(" ") else null
                    data.active = false
                    data.revokedBy = sender.uuid
                    data.revokedAt = Instant.now()
                    data.revokeReason = revokeReason
                    brennon.databaseManager.punishments.save(data).thenRun {
                        sender.sendMessage(TextUtil.success("Revoked punishment <white>$id</white>."))
                    }
                }
                else -> {
                    sender.sendMessage(TextUtil.error("Unknown action: $action. Use: reason, duration, revoke"))
                }
            }
        }.exceptionally { error ->
            sender.sendMessage(TextUtil.error("Failed to edit punishment: ${error.message}"))
            null
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            2 -> listOf("reason", "duration", "revoke")
                .filter { it.startsWith(args[1].lowercase()) }
            3 -> if (args[1].equals("duration", true)) {
                listOf("1h", "1d", "7d", "30d", "perm")
                    .filter { it.startsWith(args[2].lowercase()) }
            } else emptyList()
            else -> emptyList()
        }
    }
}
