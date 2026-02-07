package com.envarcade.brennon.core.command

import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for all Brennon commands.
 *
 * Platform plugins (Bukkit/Velocity) register these commands
 * into their respective command systems and delegate execution here.
 */
class CommandRegistry {

    private val commands = ConcurrentHashMap<String, BrennonCommand>()

    /**
     * Registers a command.
     */
    fun register(command: BrennonCommand) {
        commands[command.name.lowercase()] = command
        for (alias in command.aliases) {
            commands[alias.lowercase()] = command
        }
    }

    /**
     * Gets a command by name or alias.
     */
    fun getCommand(name: String): BrennonCommand? = commands[name.lowercase()]

    /**
     * Gets all unique registered commands (no duplicates from aliases).
     */
    fun getCommands(): Collection<BrennonCommand> = commands.values.toSet()

    /**
     * Gets all command names (including aliases).
     */
    fun getCommandNames(): Set<String> = commands.keys.toSet()
}
