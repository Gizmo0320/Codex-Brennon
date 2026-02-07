package com.envarcade.brennon.forge

import com.envarcade.brennon.api.Platform
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommandSender
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component as MCComponent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLPaths
import java.util.UUID

/**
 * Brennon Network Core — Forge Mod.
 *
 * Bridges the Brennon core into Forge's event bus and
 * command system. Uses Adventure platform adapters to
 * translate Component -> Forge's native text components.
 */
@Mod("brennon")
class BrennonForge {

    lateinit var brennon: Brennon
        private set

    private var server: MinecraftServer? = null

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        server = event.server
        val dataFolder = FMLPaths.CONFIGDIR.get().resolve("brennon").toFile()
        dataFolder.mkdirs()

        brennon = Brennon(Platform.FORGE, dataFolder)
        brennon.enable()

        // Platform hooks
        brennon.corePlayerManager.messageSender = { uuid, component ->
            val player = server?.playerList?.getPlayer(uuid)
            player?.let { sendAdventureMessage(it, component) }
        }

        brennon.coreServerManager.localPlayerCountProvider = {
            server?.playerCount ?: 0
        }
        brennon.coreServerManager.localHostProvider = { brennon.config.serverHost }
        brennon.coreServerManager.localPortProvider = { brennon.config.serverPort }

        // Set chat manager hooks
        if (brennon.config.modules.chat) {
            brennon.coreChatManager.localMessageSender = { uuid, component ->
                val player = server?.playerList?.getPlayer(uuid)
                player?.let { sendAdventureMessage(it, component) }
            }
        }

        println("[Brennon] Forge mod loaded successfully.")
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        brennon.disable()
        server = null
        println("[Brennon] Forge mod unloaded.")
    }

    @SubscribeEvent
    fun onPlayerJoin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val ip = player.connection.connection.getRemoteAddress()?.toString()
            ?.removePrefix("/")?.split(":")?.first() ?: "unknown"

        brennon.corePlayerManager.handleJoin(
            uuid = player.uuid,
            name = player.gameProfile.name,
            server = brennon.config.serverName,
            ip = ip
        )

        // Pre-load stats
        if (brennon.config.modules.stats) {
            brennon.coreStatsManager.handlePlayerJoin(player.uuid)
        }
    }

    @SubscribeEvent
    fun onPlayerQuit(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val uuid = player.uuid

        brennon.corePlayerManager.handleQuit(uuid)

        if (brennon.config.modules.stats) {
            brennon.coreStatsManager.handlePlayerQuit(uuid)
        }
        if (brennon.config.modules.chat) {
            brennon.coreChatManager.handlePlayerQuit(uuid)
        }
    }

    /**
     * Handles server chat: routes through CoreChatManager for mute enforcement
     * and channel routing when the chat module is enabled.
     */
    @SubscribeEvent
    fun onChat(event: ServerChatEvent) {
        val player = event.player
        val uuid = player.uuid
        val message = event.message.string

        if (brennon.config.modules.chat) {
            event.isCanceled = true
            val channel = brennon.coreChatManager.getPlayerChannel(uuid)
            val networkPlayer = brennon.corePlayerManager.getCachedPlayer(uuid)
            val senderName = networkPlayer?.name ?: player.gameProfile.name
            brennon.coreChatManager.sendMessage(uuid, senderName, channel.id, message)
            return
        }

        // Fallback: basic mute check
        try {
            val isMuted = brennon.corePunishmentManager.isMuted(uuid).join()
            if (isMuted) {
                event.isCanceled = true
                sendAdventureMessage(player, com.envarcade.brennon.common.util.TextUtil.error("You are muted and cannot chat."))
            }
        } catch (e: Exception) {
            println("[Brennon] Error checking mute for ${player.gameProfile.name}: ${e.message}")
        }
    }

    /**
     * Registers Brennon commands as Brigadier commands.
     */
    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        if (!::brennon.isInitialized) return

        val dispatcher = event.dispatcher
        for (command in brennon.commandRegistry.getCommands()) {
            val literal = LiteralArgumentBuilder.literal<CommandSourceStack>(command.name)
                .requires { src -> command.permission.isEmpty() || src.hasPermission(2) }
                .executes { ctx ->
                    val sender = wrapCommandSource(ctx.source)
                    command.execute(sender, emptyArray())
                    1
                }
                .then(
                    RequiredArgumentBuilder.argument<CommandSourceStack, String>("args", StringArgumentType.greedyString())
                        .suggests { ctx, builder ->
                            val input = try { StringArgumentType.getString(ctx, "args") } catch (_: Exception) { "" }
                            val args = if (input.isEmpty()) arrayOf("") else input.split(" ").toTypedArray()
                            val sender = wrapCommandSource(ctx.source)
                            for (suggestion in command.tabComplete(sender, args)) {
                                builder.suggest(suggestion)
                            }
                            builder.buildFuture()
                        }
                        .executes { ctx ->
                            val input = StringArgumentType.getString(ctx, "args")
                            val args = input.split(" ").toTypedArray()
                            val sender = wrapCommandSource(ctx.source)
                            command.execute(sender, args)
                            1
                        }
                )

            dispatcher.register(literal)

            // Register aliases
            for (alias in command.aliases) {
                val aliasLiteral = LiteralArgumentBuilder.literal<CommandSourceStack>(alias)
                    .redirect(dispatcher.root.getChild(command.name))
                dispatcher.register(aliasLiteral)
            }
        }

        println("[Brennon] Registered ${brennon.commandRegistry.getCommands().size} Brigadier commands.")
    }

    /**
     * Wraps a Forge CommandSourceStack into a BrennonCommandSender.
     */
    private fun wrapCommandSource(source: CommandSourceStack): BrennonCommandSender {
        val player = try { source.playerOrException } catch (_: Exception) { null }
        return object : BrennonCommandSender {
            override val name: String = player?.gameProfile?.name ?: "CONSOLE"
            override val uuid: UUID? = player?.uuid
            override fun sendMessage(component: Component) {
                if (player !== null) {
                    sendAdventureMessage(player, component)
                } else {
                    val plain = PlainTextComponentSerializer.plainText().serialize(component)
                    println("[Brennon] $plain")
                }
            }
            override fun hasPermission(permission: String): Boolean {
                return source.hasPermission(2)
            }
        }
    }

    /**
     * Converts an Adventure Component to Forge's native text component
     * and sends it to a player.
     */
    private fun sendAdventureMessage(player: ServerPlayer, component: Component) {
        try {
            val json = GsonComponentSerializer.gson().serialize(component)
            val mcComponent = MCComponent.Serializer.fromJson(json)
            if (mcComponent !== null) {
                player.sendSystemMessage(mcComponent)
            }
        } catch (e: Exception) {
            // Fallback to plain text
            val plain = PlainTextComponentSerializer.plainText().serialize(component)
            player.sendSystemMessage(MCComponent.literal(plain))
        }
    }
}
