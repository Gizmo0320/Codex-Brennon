package com.envarcade.brennon.core.command

import net.kyori.adventure.text.Component
import java.util.UUID

/**
 * Platform-agnostic command sender abstraction.
 *
 * Wraps Bukkit CommandSender / Velocity CommandSource so that
 * command logic in brennon-core doesn't depend on any platform.
 */
interface BrennonCommandSender {

    /** The sender's name ("CONSOLE" for console). */
    val name: String

    /** The sender's UUID, or null for console. */
    val uuid: UUID?

    /** Whether this sender is the console. */
    val isConsole: Boolean get() = uuid == null

    /** Whether this sender is a player. */
    val isPlayer: Boolean get() = uuid != null

    /** Sends a rich component message. */
    fun sendMessage(component: Component)

    /** Checks if the sender has a permission. */
    fun hasPermission(permission: String): Boolean
}

/**
 * A registered Brennon command.
 */
abstract class BrennonCommand(
    val name: String,
    val permission: String,
    val aliases: List<String> = emptyList(),
    val usage: String = "/$name",
    val description: String = ""
) {
    /**
     * Executes the command.
     *
     * @param sender The command sender
     * @param args   The command arguments
     */
    abstract fun execute(sender: BrennonCommandSender, args: Array<String>)

    /**
     * Returns tab-completion suggestions.
     */
    open fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> = emptyList()
}
