package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender

/**
 * /eco <give|take|set|balance> <player> [amount]
 */
class EconomyCommand(private val brennon: Brennon) : BrennonCommand(
    name = "eco",
    permission = "brennon.command.eco",
    aliases = listOf("economy", "bal"),
    usage = "/eco <give|take|set|balance> <player> [amount]",
    description = "Manage player economy"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sendHelp(sender)
            return
        }

        when (args[0].lowercase()) {
            "give", "deposit" -> handleGive(sender, args)
            "take", "withdraw" -> handleTake(sender, args)
            "set" -> handleSet(sender, args)
            "balance", "bal" -> handleBalance(sender, args)
            else -> sendHelp(sender)
        }
    }

    private fun handleGive(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(TextUtil.error("Usage: /eco give <player> <amount>"))
            return
        }

        val amount = args[2].toDoubleOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage(TextUtil.error("Invalid amount: ${args[2]}"))
            return
        }

        brennon.corePlayerManager.getPlayer(args[1]).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '${args[1]}' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.coreEconomyManager.deposit(target.uniqueId, amount).thenAccept { newBalance ->
                sender.sendMessage(TextUtil.success(
                    "Gave <white>$${"%.2f".format(amount)}</white> to <white>${target.name}</white>. New balance: <white>$${"%.2f".format(newBalance)}</white>"
                ))
            }
        }
    }

    private fun handleTake(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(TextUtil.error("Usage: /eco take <player> <amount>"))
            return
        }

        val amount = args[2].toDoubleOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage(TextUtil.error("Invalid amount: ${args[2]}"))
            return
        }

        brennon.corePlayerManager.getPlayer(args[1]).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '${args[1]}' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.coreEconomyManager.withdraw(target.uniqueId, amount).thenAccept { newBalance ->
                sender.sendMessage(TextUtil.success(
                    "Took <white>$${"%.2f".format(amount)}</white> from <white>${target.name}</white>. New balance: <white>$${"%.2f".format(newBalance)}</white>"
                ))
            }.exceptionally { error ->
                sender.sendMessage(TextUtil.error("Failed: ${error.cause?.message ?: error.message}"))
                null
            }
        }
    }

    private fun handleSet(sender: BrennonCommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(TextUtil.error("Usage: /eco set <player> <amount>"))
            return
        }

        val amount = args[2].toDoubleOrNull()
        if (amount == null || amount < 0) {
            sender.sendMessage(TextUtil.error("Invalid amount: ${args[2]}"))
            return
        }

        brennon.corePlayerManager.getPlayer(args[1]).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '${args[1]}' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.coreEconomyManager.setBalance(target.uniqueId, amount).thenRun {
                sender.sendMessage(TextUtil.success(
                    "Set <white>${target.name}</white>'s balance to <white>$${"%.2f".format(amount)}</white>."
                ))
            }
        }
    }

    private fun handleBalance(sender: BrennonCommandSender, args: Array<String>) {
        val targetName = if (args.size > 1) args[1] else sender.name

        brennon.corePlayerManager.getPlayer(targetName).thenAccept { opt ->
            if (opt.isEmpty) {
                sender.sendMessage(TextUtil.error("Player '$targetName' not found."))
                return@thenAccept
            }

            val target = opt.get()
            brennon.coreEconomyManager.getBalance(target.uniqueId).thenAccept { balance ->
                sender.sendMessage(TextUtil.prefixed(
                    "<white>${target.name}</white>'s balance: <green>$${"%.2f".format(balance)}</green>"
                ))
            }
        }
    }

    private fun sendHelp(sender: BrennonCommandSender) {
        sender.sendMessage(TextUtil.prefixed("Economy Commands:"))
        sender.sendMessage(TextUtil.parse("  <yellow>/eco give <player> <amount></yellow> <gray>— Give money"))
        sender.sendMessage(TextUtil.parse("  <yellow>/eco take <player> <amount></yellow> <gray>— Take money"))
        sender.sendMessage(TextUtil.parse("  <yellow>/eco set <player> <amount></yellow> <gray>— Set balance"))
        sender.sendMessage(TextUtil.parse("  <yellow>/eco balance [player]</yellow> <gray>— Check balance"))
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("give", "take", "set", "balance")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> brennon.corePlayerManager.getOnlinePlayers().map { it.getName() }
                .filter { it.lowercase().startsWith(args[1].lowercase()) }
            else -> emptyList()
        }
    }
}

/**
 * /server [name] — List servers or send player to a server
 */
class ServerCommand(private val brennon: Brennon) : BrennonCommand(
    name = "server",
    permission = "brennon.command.server",
    aliases = listOf("servers", "hub", "lobby"),
    usage = "/server [name]",
    description = "View servers or switch to one"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            // List servers
            val servers = brennon.coreServerManager.getOnlineServers()
            if (servers.isEmpty()) {
                sender.sendMessage(TextUtil.prefixed("No servers online."))
                return
            }

            sender.sendMessage(TextUtil.prefixed("Online Servers:"))
            val grouped = servers.groupBy { it.group }
            for ((group, groupServers) in grouped) {
                sender.sendMessage(TextUtil.parse("  <yellow>$group</yellow>:"))
                for (server in groupServers) {
                    val color = when {
                        server.playerCount >= server.maxPlayers -> "<red>"
                        server.playerCount >= server.maxPlayers * 0.8 -> "<gold>"
                        else -> "<green>"
                    }
                    sender.sendMessage(TextUtil.parse(
                        "    <gray>- <white>${server.name}</white> $color[${server.playerCount}/${server.maxPlayers}]"
                    ))
                }
            }

            val total = brennon.coreServerManager.getOnlineServers().sumOf { it.playerCount }
            sender.sendMessage(TextUtil.parse("  <gray>Total: <white>$total</white> players across <white>${servers.size}</white> servers"))
            return
        }

        // Send player to server
        if (!sender.isPlayer) {
            sender.sendMessage(TextUtil.error("Only players can switch servers."))
            return
        }

        val serverName = args[0]
        brennon.coreServerManager.sendPlayer(sender.uuid!!, serverName).thenRun {
            sender.sendMessage(TextUtil.success("Sending you to <white>$serverName</white>..."))
        }.exceptionally { error ->
            sender.sendMessage(TextUtil.error("Failed: ${error.cause?.message ?: error.message}"))
            null
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return brennon.coreServerManager.getOnlineServers().map { it.name }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
