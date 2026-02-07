package com.envarcade.brennon.discord

import com.envarcade.brennon.common.config.DiscordConfig
import com.envarcade.brennon.core.Brennon
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent

/**
 * Brennon Discord Integration.
 *
 * Provides Discord <-> Minecraft chat relay, punishment mirroring,
 * and player count activity updates.
 *
 * Should be initialized after Brennon core is enabled.
 */
class BrennonDiscord(
    private val brennon: Brennon,
    private val config: DiscordConfig
) {

    lateinit var jda: JDA
        private set

    private lateinit var chatRelay: MinecraftChatRelay
    private lateinit var playerCountUpdater: PlayerCountUpdater

    fun initialize() {
        if (!config.enabled || config.token.isEmpty()) {
            println("[Brennon] Discord integration is disabled or token not set.")
            return
        }

        println("[Brennon] Starting Discord bot...")

        jda = JDABuilder.createDefault(config.token)
            .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
            .addEventListeners(DiscordListener(brennon, config))
            .build()

        jda.awaitReady()
        println("[Brennon] Discord bot connected as ${jda.selfUser.name}.")

        // Start Minecraft -> Discord chat relay
        chatRelay = MinecraftChatRelay(brennon, jda, config)
        chatRelay.initialize()

        // Start player count updater
        playerCountUpdater = PlayerCountUpdater(brennon, jda)
        playerCountUpdater.start()

        println("[Brennon] Discord integration fully initialized.")
    }

    fun shutdown() {
        if (::playerCountUpdater.isInitialized) {
            playerCountUpdater.stop()
        }
        if (::chatRelay.isInitialized) {
            chatRelay.shutdown()
        }
        if (::jda.isInitialized) {
            jda.shutdown()
            println("[Brennon] Discord bot disconnected.")
        }
    }
}
