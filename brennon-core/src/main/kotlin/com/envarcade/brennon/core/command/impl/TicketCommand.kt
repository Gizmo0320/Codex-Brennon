package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.common.util.TimeUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender

/**
 * /ticket <create|view|reply|assign|close|list> — Support ticket management
 */
class TicketCommand(private val brennon: Brennon) : BrennonCommand(
    name = "ticket",
    permission = "brennon.command.ticket",
    aliases = listOf("tickets", "t"),
    usage = "/ticket <create|view|reply|assign|close|list>",
    description = "Manage support tickets"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.prefixed("Ticket Commands:"))
            sender.sendMessage(TextUtil.parse("  <yellow>/ticket create <subject> <dark_gray>- Create a ticket"))
            sender.sendMessage(TextUtil.parse("  <yellow>/ticket view <id> <dark_gray>- View a ticket"))
            sender.sendMessage(TextUtil.parse("  <yellow>/ticket reply <id> <message> <dark_gray>- Reply to a ticket"))
            sender.sendMessage(TextUtil.parse("  <yellow>/ticket close <id> <dark_gray>- Close a ticket"))
            sender.sendMessage(TextUtil.parse("  <yellow>/ticket list [mine|open|assigned] <dark_gray>- List tickets"))
            if (sender.hasPermission("brennon.staff.ticket.assign")) {
                sender.sendMessage(TextUtil.parse("  <yellow>/ticket assign <id> <player> <dark_gray>- Assign a ticket"))
            }
            return
        }

        val ticketManager = brennon.coreTicketManager

        when (args[0].lowercase()) {
            "create" -> {
                if (!sender.isPlayer) {
                    sender.sendMessage(TextUtil.error("Only players can create tickets."))
                    return
                }
                if (args.size < 2) {
                    sender.sendMessage(TextUtil.error("Usage: /ticket create <subject>"))
                    return
                }
                val subject = args.drop(1).joinToString(" ")
                ticketManager.createTicket(
                    sender.uuid!!, sender.name, subject, subject, brennon.config.serverName
                ).thenAccept { ticket ->
                    sender.sendMessage(TextUtil.success("Ticket <yellow>${ticket.id}</yellow> created."))
                }.exceptionally { e ->
                    sender.sendMessage(TextUtil.error("Failed to create ticket: ${e.message}"))
                    null
                }
            }

            "view" -> {
                if (args.size < 2) {
                    sender.sendMessage(TextUtil.error("Usage: /ticket view <id>"))
                    return
                }
                val id = args[1].uppercase()
                ticketManager.getTicket(id).thenAccept { opt ->
                    if (opt.isEmpty) {
                        sender.sendMessage(TextUtil.error("Ticket not found: <white>$id"))
                        return@thenAccept
                    }
                    val ticket = opt.get()
                    sender.sendMessage(TextUtil.prefixed("Ticket <yellow>${ticket.id}"))
                    sender.sendMessage(TextUtil.parse("  <gray>Subject: <white>${ticket.subject}"))
                    sender.sendMessage(TextUtil.parse("  <gray>Status: <yellow>${ticket.status} <gray>Priority: <yellow>${ticket.priority}"))
                    sender.sendMessage(TextUtil.parse("  <gray>Created by: <white>${ticket.creatorName} <gray>on <white>${ticket.server}"))
                    if (ticket.assignee != null) {
                        sender.sendMessage(TextUtil.parse("  <gray>Assigned to: <white>${ticket.assignee}"))
                    }
                    sender.sendMessage(TextUtil.parse("  <gray>Messages:"))
                    for (msg in ticket.messages) {
                        val prefix = if (msg.isStaffMessage) "<red>[Staff]</red> " else ""
                        sender.sendMessage(TextUtil.parse("    $prefix<yellow>${msg.authorName}<dark_gray>: <white>${msg.content}"))
                    }
                }
            }

            "reply" -> {
                if (!sender.isPlayer) {
                    sender.sendMessage(TextUtil.error("Only players can reply to tickets."))
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage(TextUtil.error("Usage: /ticket reply <id> <message>"))
                    return
                }
                val id = args[1].uppercase()
                val message = args.drop(2).joinToString(" ")
                val isStaff = sender.hasPermission("brennon.staff.ticket")
                ticketManager.addReply(id, sender.uuid!!, sender.name, message, isStaff).thenRun {
                    sender.sendMessage(TextUtil.success("Reply added to ticket <yellow>$id</yellow>."))
                }.exceptionally { e ->
                    sender.sendMessage(TextUtil.error("Failed to reply: ${e.message}"))
                    null
                }
            }

            "assign" -> {
                if (!sender.hasPermission("brennon.staff.ticket.assign")) {
                    sender.sendMessage(TextUtil.error("You don't have permission to assign tickets."))
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage(TextUtil.error("Usage: /ticket assign <id> <player>"))
                    return
                }
                val id = args[1].uppercase()
                val targetName = args[2]
                brennon.corePlayerManager.getPlayer(targetName).thenAccept { optPlayer ->
                    if (optPlayer.isEmpty) {
                        sender.sendMessage(TextUtil.error("Player not found: <white>$targetName"))
                        return@thenAccept
                    }
                    ticketManager.assignTicket(id, optPlayer.get().uniqueId).thenRun {
                        sender.sendMessage(TextUtil.success("Ticket <yellow>$id</yellow> assigned to <yellow>$targetName</yellow>."))
                    }
                }
            }

            "close" -> {
                if (!sender.isPlayer) {
                    sender.sendMessage(TextUtil.error("Only players can close tickets."))
                    return
                }
                if (args.size < 2) {
                    sender.sendMessage(TextUtil.error("Usage: /ticket close <id>"))
                    return
                }
                val id = args[1].uppercase()
                ticketManager.closeTicket(id, sender.uuid!!).thenRun {
                    sender.sendMessage(TextUtil.success("Ticket <yellow>$id</yellow> closed."))
                }.exceptionally { e ->
                    sender.sendMessage(TextUtil.error("Failed to close ticket: ${e.message}"))
                    null
                }
            }

            "list" -> {
                val filter = if (args.size > 1) args[1].lowercase() else if (sender.hasPermission("brennon.staff.ticket")) "open" else "mine"
                when (filter) {
                    "mine" -> {
                        if (!sender.isPlayer) {
                            sender.sendMessage(TextUtil.error("Only players can list their tickets."))
                            return
                        }
                        ticketManager.getPlayerTickets(sender.uuid!!).thenAccept { tickets ->
                            if (tickets.isEmpty()) {
                                sender.sendMessage(TextUtil.prefixed("You have no tickets."))
                                return@thenAccept
                            }
                            sender.sendMessage(TextUtil.prefixed("Your Tickets:"))
                            for (ticket in tickets.take(15)) {
                                sender.sendMessage(TextUtil.parse("  <yellow>${ticket.id} <gray>- <white>${ticket.subject} <dark_gray>[<yellow>${ticket.status}</yellow>]"))
                            }
                        }
                    }
                    "open" -> {
                        ticketManager.getOpenTickets().thenAccept { tickets ->
                            if (tickets.isEmpty()) {
                                sender.sendMessage(TextUtil.prefixed("No open tickets."))
                                return@thenAccept
                            }
                            sender.sendMessage(TextUtil.prefixed("Open Tickets:"))
                            for (ticket in tickets.take(15)) {
                                sender.sendMessage(TextUtil.parse("  <yellow>${ticket.id} <gray>- <white>${ticket.subject} <dark_gray>by ${ticket.creatorName} [<yellow>${ticket.status}</yellow>]"))
                            }
                        }
                    }
                    "assigned" -> {
                        if (!sender.isPlayer) {
                            sender.sendMessage(TextUtil.error("Only players can list assigned tickets."))
                            return
                        }
                        ticketManager.getAssignedTickets(sender.uuid!!).thenAccept { tickets ->
                            if (tickets.isEmpty()) {
                                sender.sendMessage(TextUtil.prefixed("No assigned tickets."))
                                return@thenAccept
                            }
                            sender.sendMessage(TextUtil.prefixed("Assigned Tickets:"))
                            for (ticket in tickets.take(15)) {
                                sender.sendMessage(TextUtil.parse("  <yellow>${ticket.id} <gray>- <white>${ticket.subject} <dark_gray>by ${ticket.creatorName}"))
                            }
                        }
                    }
                    else -> sender.sendMessage(TextUtil.error("Unknown filter: $filter. Use: mine, open, assigned"))
                }
            }

            else -> sender.sendMessage(TextUtil.error("Unknown subcommand: ${args[0]}"))
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            val subs = mutableListOf("create", "view", "reply", "close", "list")
            if (sender.hasPermission("brennon.staff.ticket.assign")) subs.add("assign")
            return subs.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        if (args.size == 2 && args[0].lowercase() == "list") {
            return listOf("mine", "open", "assigned").filter { it.startsWith(args[1], ignoreCase = true) }
        }
        return emptyList()
    }
}
