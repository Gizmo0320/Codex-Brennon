package com.envarcade.brennon.proxy.command

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Bridges Brennon commands into Velocity's command system.
 */
class VelocityCommandBridge(
    private val brennon: Brennon,
    private val proxy: ProxyServer,
    private val plugin: Any
) {

    fun registerAll() {
        val commandManager = proxy.commandManager

        for (command in brennon.commandRegistry.getCommands()) {
            val velocityCmd = VelocityCommandWrapper(command)
            val meta = commandManager.metaBuilder(command.name)
                .aliases(*command.aliases.toTypedArray())
                .plugin(plugin)
                .build()
            commandManager.register(meta, velocityCmd)
        }
    }

    private class VelocityCommandWrapper(private val command: BrennonCommand) : SimpleCommand {

        override fun execute(invocation: SimpleCommand.Invocation) {
            val sender = wrapSource(invocation.source())
            if (!sender.hasPermission(command.permission)) {
                sender.sendMessage(Component.text("§cYou don't have permission to use this command."))
                return
            }
            command.execute(sender, invocation.arguments())
        }

        override fun suggestAsync(invocation: SimpleCommand.Invocation): CompletableFuture<List<String>> {
            val sender = wrapSource(invocation.source())
            if (!sender.hasPermission(command.permission)) return CompletableFuture.completedFuture(emptyList())
            return CompletableFuture.completedFuture(command.tabComplete(sender, invocation.arguments()))
        }

        override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
            return invocation.source().hasPermission(command.permission)
        }
    }
}

private fun wrapSource(source: com.velocitypowered.api.command.CommandSource): BrennonCommandSender {
    return object : BrennonCommandSender {
        override val name: String = if (source is Player) source.username else "CONSOLE"
        override val uuid: UUID? = (source as? Player)?.uniqueId
        override fun sendMessage(component: Component) = source.sendMessage(component)
        override fun hasPermission(permission: String): Boolean = source.hasPermission(permission)
    }
}
