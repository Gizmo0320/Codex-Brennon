package com.envarcade.brennon.proxy

import com.envarcade.brennon.api.Platform
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.proxy.command.VelocityCommandBridge
import com.envarcade.brennon.proxy.listener.ProxyChatListener
import com.envarcade.brennon.proxy.listener.ProxyPlayerListener
import com.envarcade.brennon.proxy.server.VelocityServerSync
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

@Plugin(
    id = "brennon",
    name = "Brennon",
    version = Brennon.VERSION,
    description = "Brennon Network Core System",
    authors = ["Envarcade"]
)
class BrennonVelocity @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory val dataDirectory: Path
) {

    lateinit var brennon: Brennon
        private set
    private lateinit var serverSync: VelocityServerSync

    @Subscribe
    fun onProxyInit(event: ProxyInitializeEvent) {
        brennon = Brennon(Platform.VELOCITY, dataDirectory.toFile())
        brennon.enable()

        // Platform hooks
        brennon.corePlayerManager.messageSender = { uuid, component ->
            server.getPlayer(uuid).ifPresent { it.sendMessage(component) }
        }
        brennon.coreServerManager.localPlayerCountProvider = { server.playerCount }
        brennon.coreServerManager.playerSender = { uuid, serverName ->
            val future = CompletableFuture<Void>()
            val player = server.getPlayer(uuid).orElse(null)
            var target = server.getServer(serverName).orElse(null)

            if (player == null) {
                future.completeExceptionally(IllegalStateException("Player $uuid is not online."))
            } else if (target == null) {
                // Try on-demand registration from the registry
                val def = brennon.serverRegistryService.getServerDefinition(serverName)
                if (def != null) {
                    serverSync.onServerRegistered(def)
                    target = server.getServer(serverName).orElse(null)
                }
                if (target != null) {
                    player.createConnectionRequest(target).fireAndForget()
                    future.complete(null)
                } else {
                    future.completeExceptionally(IllegalStateException("Server '$serverName' not found."))
                }
            } else {
                player.createConnectionRequest(target).fireAndForget()
                future.complete(null)
            }
            future
        }

        // Initialize server sync (dynamic registration with Velocity)
        serverSync = VelocityServerSync(
            proxy = server,
            registryService = brennon.serverRegistryService,
            serverManager = brennon.coreServerManager,
            eventBus = brennon.coreEventBus,
            config = brennon.config
        )
        serverSync.initialize()

        // Wire auto-registration callback
        brennon.coreServerManager.autoRegistrationCallback = { name, group, host, port ->
            serverSync.onFirstHeartbeat(name, group, host, port)
        }

        // Set chat manager hooks
        if (brennon.config.modules.chat) {
            brennon.coreChatManager.localMessageSender = { uuid, component ->
                server.getPlayer(uuid).ifPresent { it.sendMessage(component) }
            }
        }

        // Subscribe to staff alerts (punishment notifications)
        brennon.redisMessaging.subscribe(com.envarcade.brennon.messaging.channel.Channels.STAFF_ALERT) { _, message ->
            try {
                val json = com.google.gson.JsonParser.parseString(message).asJsonObject
                if (json.get("type")?.asString == "punishment") {
                    val action = json.get("action")?.asString ?: return@subscribe
                    val punishmentType = json.get("punishmentType")?.asString ?: return@subscribe
                    val target = json.get("target")?.asString ?: return@subscribe
                    val targetName = brennon.corePlayerManager.getCachedPlayer(
                        java.util.UUID.fromString(target)
                    )?.getName() ?: target

                    val alertComponent = if (action == "issued") {
                        val issuer = json.get("issuer")?.asString ?: "CONSOLE"
                        val reason = json.get("reason")?.asString ?: ""
                        net.kyori.adventure.text.Component.text("[Staff] ", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                            .append(net.kyori.adventure.text.Component.text("$punishmentType ", net.kyori.adventure.text.format.NamedTextColor.RED))
                            .append(net.kyori.adventure.text.Component.text("issued to ", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                            .append(net.kyori.adventure.text.Component.text("$targetName ", net.kyori.adventure.text.format.NamedTextColor.WHITE))
                            .append(net.kyori.adventure.text.Component.text("by ", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                            .append(net.kyori.adventure.text.Component.text("$issuer: ", net.kyori.adventure.text.format.NamedTextColor.WHITE))
                            .append(net.kyori.adventure.text.Component.text(reason, net.kyori.adventure.text.format.NamedTextColor.GRAY))
                    } else {
                        val revokedBy = json.get("revokedBy")?.asString ?: "CONSOLE"
                        net.kyori.adventure.text.Component.text("[Staff] ", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                            .append(net.kyori.adventure.text.Component.text("$punishmentType ", net.kyori.adventure.text.format.NamedTextColor.GREEN))
                            .append(net.kyori.adventure.text.Component.text("revoked for ", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                            .append(net.kyori.adventure.text.Component.text("$targetName ", net.kyori.adventure.text.format.NamedTextColor.WHITE))
                            .append(net.kyori.adventure.text.Component.text("by ", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                            .append(net.kyori.adventure.text.Component.text(revokedBy, net.kyori.adventure.text.format.NamedTextColor.WHITE))
                    }

                    for (player in server.allPlayers) {
                        if (player.hasPermission("brennon.staff.alerts")) {
                            player.sendMessage(alertComponent)
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Register listeners
        server.eventManager.register(this, ProxyPlayerListener(brennon, server))
        if (brennon.config.modules.chat) {
            server.eventManager.register(this, ProxyChatListener(brennon, server))
        }

        // Register commands
        VelocityCommandBridge(brennon, server, this).registerAll()

        logger.info("[Brennon] Velocity plugin loaded. ${brennon.commandRegistry.getCommands().size} commands registered.")
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        serverSync.shutdown()
        brennon.disable()
        logger.info("[Brennon] Velocity plugin unloaded.")
    }
}
