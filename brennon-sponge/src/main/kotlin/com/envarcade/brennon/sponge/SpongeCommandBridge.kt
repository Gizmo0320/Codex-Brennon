package com.envarcade.brennon.sponge

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommandSender
import net.kyori.adventure.text.Component
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.Command
import org.spongepowered.api.command.CommandCause
import org.spongepowered.api.command.CommandCompletion
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.parameter.ArgumentReader
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.plugin.PluginContainer
import java.util.Optional
import java.util.UUID

/**
 * Bridges Brennon commands to Sponge's command system.
 */
class SpongeCommandBridge(
    private val brennon: Brennon,
    private val plugin: PluginContainer
) {

    fun registerAll() {
        val registrar = Sponge.server().commandManager()

        for (command in brennon.commandRegistry.getCommands()) {
            val spongeCommand = object : Command.Raw {
                override fun process(cause: CommandCause, arguments: ArgumentReader.Mutable): CommandResult {
                    val sender = wrapCause(cause)
                    val args = parseArgs(arguments.input())
                    command.execute(sender, args)
                    return CommandResult.success()
                }

                override fun complete(cause: CommandCause, arguments: ArgumentReader.Mutable): List<CommandCompletion> {
                    val sender = wrapCause(cause)
                    val args = parseArgs(arguments.input())
                    val input = if (args.isEmpty()) arrayOf("") else args
                    return command.tabComplete(sender, input).map { CommandCompletion.of(it) }
                }

                override fun canExecute(cause: CommandCause): Boolean {
                    return command.permission.isEmpty() || cause.hasPermission(command.permission)
                }

                override fun shortDescription(cause: CommandCause): Optional<Component> {
                    return Optional.of(Component.text(command.description.ifEmpty { command.name }))
                }

                override fun extendedDescription(cause: CommandCause): Optional<Component> {
                    return Optional.of(Component.text(command.description.ifEmpty { command.usage }))
                }

                override fun usage(cause: CommandCause): Component {
                    return Component.text(command.usage)
                }
            }

            registrar.registrar(Command.Raw::class.java).ifPresent { reg ->
                reg.register(plugin, spongeCommand, command.name, *command.aliases.toTypedArray())
            }
        }
    }

    private fun wrapCause(cause: CommandCause): BrennonCommandSender {
        val player = cause.subject() as? ServerPlayer
        return object : BrennonCommandSender {
            override val name: String = player?.name() ?: "CONSOLE"
            override val uuid: UUID? = player?.uniqueId()
            override fun sendMessage(component: Component) {
                cause.audience().sendMessage(component)
            }
            override fun hasPermission(permission: String): Boolean {
                return cause.hasPermission(permission)
            }
        }
    }

    private fun parseArgs(input: String): Array<String> {
        val parts = input.trim().split("\\s+".toRegex())
        // First element is the command name itself, skip it
        return if (parts.size > 1) parts.drop(1).toTypedArray() else emptyArray()
    }
}
