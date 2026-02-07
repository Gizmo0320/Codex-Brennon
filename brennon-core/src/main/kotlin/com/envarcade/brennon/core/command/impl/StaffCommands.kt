package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.common.util.TimeUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender
import com.envarcade.brennon.core.staff.ReportManager
import com.envarcade.brennon.core.staff.StaffManager
import java.time.Instant

/**
 * /staffmode — Toggle staff mode
 */
class StaffModeCommand(
    private val brennon: Brennon,
    private val staffManager: StaffManager
) : BrennonCommand(
    name = "staffmode",
    permission = "brennon.command.staffmode",
    aliases = listOf("sm", "staff"),
    usage = "/staffmode",
    description = "Toggle staff mode"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (!sender.isPlayer) {
            sender.sendMessage(TextUtil.error("Only players can use staff mode."))
            return
        }

        staffManager.toggleStaffMode(sender.uuid!!, sender.name).thenAccept { enabled ->
            if (enabled) {
                sender.sendMessage(TextUtil.success("Staff mode <green>enabled</green>."))
                sender.sendMessage(TextUtil.parse("  <gray>Use <yellow>/vanish</yellow> to toggle visibility."))
            } else {
                sender.sendMessage(TextUtil.success("Staff mode <red>disabled</red>."))
            }
        }
    }
}

/**
 * /vanish — Toggle vanish
 */
class VanishCommand(
    private val brennon: Brennon,
    private val staffManager: StaffManager
) : BrennonCommand(
    name = "vanish",
    permission = "brennon.command.vanish",
    aliases = listOf("v"),
    usage = "/vanish",
    description = "Toggle vanish"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (!sender.isPlayer) {
            sender.sendMessage(TextUtil.error("Only players can vanish."))
            return
        }

        staffManager.toggleVanish(sender.uuid!!).thenAccept { vanished ->
            if (vanished) {
                sender.sendMessage(TextUtil.success("You are now <green>vanished</green>."))
            } else {
                sender.sendMessage(TextUtil.success("You are now <red>visible</red>."))
            }
        }
    }
}

/**
 * /staffchat <message> — Staff-only cross-server chat
 */
class StaffChatCommand(private val brennon: Brennon) : BrennonCommand(
    name = "staffchat",
    permission = "brennon.command.staffchat",
    aliases = listOf("sc"),
    usage = "/staffchat <message>",
    description = "Send a message to the staff chat"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: $usage"))
            return
        }

        val message = args.joinToString(" ")
        val server = brennon.config.serverName

        brennon.redisMessaging.publish(
            com.envarcade.brennon.messaging.channel.Channels.STAFF_CHAT,
            """{"sender":"${sender.name}","server":"$server","message":"$message"}"""
        )

        // Local echo
        sender.sendMessage(TextUtil.parse(
            "<dark_gray>[<red>SC</red>] <gray>[$server] <white>${sender.name}</white>: <gray>$message"
        ))
    }
}

/**
 * /stafflist — Show online staff across the network
 */
class StaffListCommand(
    private val brennon: Brennon,
    private val staffManager: StaffManager
) : BrennonCommand(
    name = "stafflist",
    permission = "brennon.command.stafflist",
    aliases = listOf("sl", "onlinestaff"),
    usage = "/stafflist",
    description = "List online staff across the network"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        staffManager.getNetworkStaff().thenAccept { networkStaff ->
            if (networkStaff.isEmpty()) {
                sender.sendMessage(TextUtil.prefixed("No staff members currently in staff mode."))
                return@thenAccept
            }

            sender.sendMessage(TextUtil.prefixed("Online Staff (${networkStaff.size}):"))
            for ((_, data) in networkStaff) {
                val name = data["name"] ?: "Unknown"
                val server = data["server"] ?: "?"
                val vanished = data["vanished"] == "true"
                val vanishTag = if (vanished) " <gray>(vanished)</gray>" else ""
                sender.sendMessage(TextUtil.parse("  <gray>- <white>$name</white> <dark_gray>on <yellow>$server</yellow>$vanishTag"))
            }
        }
    }
}

/**
 * /report <player> <reason> — Report a player
 */
class ReportCommand(
    private val brennon: Brennon,
    private val reportManager: ReportManager
) : BrennonCommand(
    name = "report",
    permission = "brennon.command.report",
    usage = "/report <player> <reason>",
    description = "Report a player"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (!sender.isPlayer) {
            sender.sendMessage(TextUtil.error("Only players can file reports."))
            return
        }

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
            val report = reportManager.createReport(sender.uuid!!, sender.name, target.uniqueId, target.name, reason)
            sender.sendMessage(TextUtil.success(
                "Report filed against <white>${target.name}</white>. Report ID: <white>${report.id}</white>"
            ))
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
 * /reports — View and manage open reports
 */
class ReportsCommand(
    private val brennon: Brennon,
    private val reportManager: ReportManager
) : BrennonCommand(
    name = "reports",
    permission = "brennon.command.reports",
    aliases = listOf("viewreports"),
    usage = "/reports [claim <id>|resolve <id>|dismiss <id>]",
    description = "View and manage reports"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            // List open reports
            val reports = reportManager.getOpenReports()
            if (reports.isEmpty()) {
                sender.sendMessage(TextUtil.prefixed("No open reports."))
                return
            }

            sender.sendMessage(TextUtil.prefixed("Open Reports (${reports.size}):"))
            for (report in reports.take(15)) {
                val time = TimeUtil.format(Instant.ofEpochMilli(report.timestamp))
                val statusColor = when (report.status) {
                    ReportManager.ReportStatus.OPEN -> "<red>"
                    ReportManager.ReportStatus.IN_PROGRESS -> "<yellow>"
                    else -> "<green>"
                }
                sender.sendMessage(TextUtil.parse(
                    "  <gray>[${statusColor}${report.status.name}<gray>] <white>${report.id}</white> — <yellow>${report.targetName}</yellow> by ${report.reporterName}: <gray>${report.reason} <dark_gray>($time)"
                ))
            }
            return
        }

        when (args[0].lowercase()) {
            "claim" -> {
                if (args.size < 2 || !sender.isPlayer) {
                    sender.sendMessage(TextUtil.error("Usage: /reports claim <id>"))
                    return
                }
                if (reportManager.claimReport(args[1], sender.uuid!!)) {
                    sender.sendMessage(TextUtil.success("Claimed report <white>${args[1]}</white>."))
                } else {
                    sender.sendMessage(TextUtil.error("Failed to claim report."))
                }
            }
            "resolve" -> {
                if (args.size < 2) {
                    sender.sendMessage(TextUtil.error("Usage: /reports resolve <id>"))
                    return
                }
                reportManager.resolveReport(args[1], ReportManager.ReportStatus.RESOLVED)
                sender.sendMessage(TextUtil.success("Report <white>${args[1]}</white> resolved."))
            }
            "dismiss" -> {
                if (args.size < 2) {
                    sender.sendMessage(TextUtil.error("Usage: /reports dismiss <id>"))
                    return
                }
                reportManager.resolveReport(args[1], ReportManager.ReportStatus.DISMISSED)
                sender.sendMessage(TextUtil.success("Report <white>${args[1]}</white> dismissed."))
            }
        }
    }
}
