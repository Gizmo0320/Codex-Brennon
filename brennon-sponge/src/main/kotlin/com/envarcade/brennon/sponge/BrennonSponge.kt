package com.envarcade.brennon.sponge

import com.envarcade.brennon.api.Platform
import com.envarcade.brennon.core.Brennon
import com.google.inject.Inject
import org.apache.logging.log4j.Logger
import org.spongepowered.api.Server
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.nio.file.Path

/**
 * Brennon Network Core — Sponge Plugin.
 *
 * Sponge has native Adventure support, so no text serializer
 * adapters are needed. Components are used directly.
 */
@Plugin("brennon")
class BrennonSponge @Inject constructor(
    val logger: Logger,
    @ConfigDir(sharedRoot = false) val configDir: Path,
    val pluginContainer: PluginContainer
) {

    lateinit var brennon: Brennon
        private set

    @Listener(order = Order.EARLY)
    fun onServerStarted(event: StartedEngineEvent<Server>) {
        val dataFolder = configDir.toFile()
        dataFolder.mkdirs()

        brennon = Brennon(Platform.SPONGE, dataFolder)
        brennon.enable()

        // Platform hooks — Sponge has native Adventure, so we use it directly
        brennon.corePlayerManager.messageSender = { uuid, component ->
            Sponge.server().player(uuid).ifPresent { it.sendMessage(component) }
        }
        brennon.coreServerManager.localPlayerCountProvider = { Sponge.server().onlinePlayers().size }
        brennon.coreServerManager.localHostProvider = { brennon.config.serverHost }
        brennon.coreServerManager.localPortProvider = { brennon.config.serverPort }

        // Set chat manager hooks
        if (brennon.config.modules.chat) {
            brennon.coreChatManager.localMessageSender = { uuid, component ->
                Sponge.server().player(uuid).ifPresent { it.sendMessage(component) }
            }
        }

        // Register listeners
        Sponge.eventManager().registerListeners(pluginContainer, SpongePlayerListener(brennon))
        Sponge.eventManager().registerListeners(pluginContainer, SpongeChatListener(brennon))

        // Register commands
        SpongeCommandBridge(brennon, pluginContainer).registerAll()

        logger.info("[Brennon] Sponge plugin loaded. ${brennon.commandRegistry.getCommands().size} commands registered.")
    }

    @Listener(order = Order.LATE)
    fun onServerStopping(event: StoppingEngineEvent<Server>) {
        brennon.disable()
        logger.info("[Brennon] Sponge plugin unloaded.")
    }
}
