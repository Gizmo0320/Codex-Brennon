package com.envarcade.brennon.folia.command

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender
import net.kyori.adventure.text.Component
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * Bridges Brennon commands into Bukkit's command system (Folia-compatible).
 */
class FoliaCommandBridge(private val brennon: Brennon, private val plugin: JavaPlugin) {

    /**
     * Registers all Brennon commands with Bukkit.
     * Commands must be listed in plugin.yml or registered via CommandMap.
     */
    fun registerAll() {
        // Use Bukkit's CommandMap for dynamic registration
        try {
            val server = plugin.server
            val commandMapField = server::class.java.getDeclaredMethod("getCommandMap")
            val commandMap = commandMapField.invoke(server) as org.bukkit.command.CommandMap

            for (command in brennon.commandRegistry.getCommands()) {
                val bukkitCmd = DynamicBukkitCommand(command)
                commandMap.register("brennon", bukkitCmd)
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to register commands dynamically: ${e.message}")
            println("[Brennon] Falling back to plugin.yml registration.")
            // Fallback: try plugin.yml registered commands
            for (command in brennon.commandRegistry.getCommands()) {
                val executor = BukkitCommandExecutor(command)
                plugin.getCommand(command.name)?.setExecutor(executor)
                plugin.getCommand(command.name)?.tabCompleter = executor
            }
        }
    }

    /**
     * Dynamically registered Bukkit command that delegates to a BrennonCommand.
     */
    private class DynamicBukkitCommand(private val brennonCmd: BrennonCommand) : Command(
        brennonCmd.name,
        brennonCmd.description,
        brennonCmd.usage,
        brennonCmd.aliases
    ) {
        init {
            permission = brennonCmd.permission
        }

        override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
            val wrapped = wrapSender(sender)
            if (!wrapped.hasPermission(brennonCmd.permission)) {
                sender.sendMessage(Component.text("§cYou don't have permission to use this command."))
                return true
            }
            brennonCmd.execute(wrapped, args)
            return true
        }

        override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
            val wrapped = wrapSender(sender)
            if (!wrapped.hasPermission(brennonCmd.permission)) return emptyList()
            return brennonCmd.tabComplete(wrapped, args)
        }
    }

    private class BukkitCommandExecutor(private val command: BrennonCommand) : CommandExecutor, TabCompleter {
        override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
            val wrapped = wrapSender(sender)
            if (!wrapped.hasPermission(command.permission)) {
                sender.sendMessage(Component.text("§cYou don't have permission to use this command."))
                return true
            }
            command.execute(wrapped, args)
            return true
        }

        override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<String>): List<String> {
            val wrapped = wrapSender(sender)
            if (!wrapped.hasPermission(command.permission)) return emptyList()
            return command.tabComplete(wrapped, args)
        }
    }
}

private fun wrapSender(sender: CommandSender): BrennonCommandSender {
    return object : BrennonCommandSender {
        override val name: String = sender.name
        override val uuid: UUID? = (sender as? Player)?.uniqueId
        override fun sendMessage(component: Component) = sender.sendMessage(component)
        override fun hasPermission(permission: String): Boolean = sender.hasPermission(permission)
    }
}
