package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender

/**
 * /channel <name> — Switch focused chat channel
 */
class ChannelCommand(private val brennon: Brennon) : BrennonCommand(
    name = "channel",
    permission = "brennon.command.channel",
    aliases = listOf("ch"),
    usage = "/channel [name]",
    description = "Switch chat channel"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (!sender.isPlayer) {
            sender.sendMessage(TextUtil.error("Only players can switch channels."))
            return
        }

        val chatManager = brennon.coreChatManager

        if (args.isEmpty()) {
            val current = chatManager.getPlayerChannel(sender.uuid!!)
            sender.sendMessage(TextUtil.prefixed("Current channel: <yellow>${current.displayName}"))
            sender.sendMessage(TextUtil.parse("  <gray>Available channels:"))
            for (channel in chatManager.channels) {
                val perm = channel.permission
                if (perm.isNotEmpty() && !sender.hasPermission(perm)) continue
                val indicator = if (channel.id == current.id) " <green>\u2714" else ""
                sender.sendMessage(TextUtil.parse("  <dark_gray>- <yellow>${channel.id} <gray>(${channel.displayName})$indicator"))
            }
            return
        }

        val channelId = args[0].lowercase()
        // Check by ID or shortcut
        val channel = chatManager.channels.firstOrNull {
            it.id == channelId || it.shortcut == channelId
        }
        if (channel == null) {
            sender.sendMessage(TextUtil.error("Channel not found: <white>$channelId"))
            return
        }

        val perm = channel.permission
        if (perm.isNotEmpty() && !sender.hasPermission(perm)) {
            sender.sendMessage(TextUtil.error("You don't have permission for that channel."))
            return
        }

        chatManager.setPlayerChannel(sender.uuid!!, channel.id)
        sender.sendMessage(TextUtil.success("Switched to channel: <yellow>${channel.displayName}"))
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return brennon.coreChatManager.channels
                .filter { ch ->
                    val perm = ch.permission
                    perm.isEmpty() || sender.hasPermission(perm)
                }
                .map { it.id }
                .filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}

/**
 * /msg <player> <message> — Send a private message
 */
class MsgCommand(private val brennon: Brennon) : BrennonCommand(
    name = "msg",
    permission = "brennon.command.msg",
    aliases = listOf("message", "tell", "whisper", "w"),
    usage = "/msg <player> <message>",
    description = "Send a private message"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (!sender.isPlayer) {
            sender.sendMessage(TextUtil.error("Only players can send private messages."))
            return
        }
        if (args.size < 2) {
            sender.sendMessage(TextUtil.error("Usage: /msg <player> <message>"))
            return
        }

        val targetName = args[0]
        val message = args.drop(1).joinToString(" ")

        brennon.corePlayerManager.getPlayer(targetName).thenAccept { optPlayer ->
            if (optPlayer.isEmpty) {
                sender.sendMessage(TextUtil.error("Player not found: <white>$targetName"))
                return@thenAccept
            }
            val target = optPlayer.get()
            brennon.coreChatManager.sendPrivateMessage(sender.uuid!!, sender.name, target.uniqueId, message)
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return brennon.corePlayerManager.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}

/**
 * /reply <message> — Reply to last private message
 */
class ReplyCommand(private val brennon: Brennon) : BrennonCommand(
    name = "reply",
    permission = "brennon.command.reply",
    aliases = listOf("r"),
    usage = "/reply <message>",
    description = "Reply to last private message"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (!sender.isPlayer) {
            sender.sendMessage(TextUtil.error("Only players can reply."))
            return
        }
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: /reply <message>"))
            return
        }

        val recipient = brennon.coreChatManager.lastMessageRecipients[sender.uuid!!]
        if (recipient == null) {
            sender.sendMessage(TextUtil.error("No one to reply to."))
            return
        }

        val message = args.joinToString(" ")
        brennon.coreChatManager.sendPrivateMessage(sender.uuid!!, sender.name, recipient, message)
    }
}

/**
 * /chatfilter <enable|disable|list|reload> [id] — Manage chat filters
 */
class ChatFilterCommand(private val brennon: Brennon) : BrennonCommand(
    name = "chatfilter",
    permission = "brennon.admin.chatfilter",
    usage = "/chatfilter <enable|disable|list>",
    description = "Manage chat filters"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: /chatfilter <enable|disable|list>"))
            return
        }

        when (args[0].lowercase()) {
            "list" -> {
                sender.sendMessage(TextUtil.prefixed("Chat Filters:"))
                for (channel in brennon.coreChatManager.channels) {
                    // Filters are accessed from the chat manager
                }
                sender.sendMessage(TextUtil.prefixed("Filters loaded from config."))
            }
            else -> sender.sendMessage(TextUtil.error("Unknown subcommand: ${args[0]}"))
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return listOf("enable", "disable", "list").filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}
