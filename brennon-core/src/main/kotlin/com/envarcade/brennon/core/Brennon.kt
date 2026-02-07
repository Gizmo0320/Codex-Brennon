package com.envarcade.brennon.core

import com.envarcade.brennon.api.BrennonAPI
import com.envarcade.brennon.api.BrennonProvider
import com.envarcade.brennon.api.Platform
import com.envarcade.brennon.api.chat.ChatManager
import com.envarcade.brennon.api.economy.EconomyManager
import com.envarcade.brennon.api.event.EventBus
import com.envarcade.brennon.api.gui.GuiManager
import com.envarcade.brennon.api.messaging.MessagingService
import com.envarcade.brennon.api.module.ModuleManager
import com.envarcade.brennon.api.player.PlayerManager
import com.envarcade.brennon.api.punishment.PunishmentManager
import com.envarcade.brennon.api.rank.RankManager
import com.envarcade.brennon.api.server.ServerManager
import com.envarcade.brennon.api.stats.StatsManager
import com.envarcade.brennon.api.ticket.TicketManager
import com.envarcade.brennon.common.config.BrennonConfig
import com.envarcade.brennon.common.config.ConfigLoader
import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.core.chat.CoreChatManager
import com.envarcade.brennon.core.command.CommandRegistry
import com.envarcade.brennon.core.command.impl.*
import com.envarcade.brennon.core.economy.CoreEconomyManager
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.gui.CoreGuiManager
import com.envarcade.brennon.core.module.CoreModuleManager
import com.envarcade.brennon.core.player.CorePlayerManager
import com.envarcade.brennon.core.punishment.CorePunishmentManager
import com.envarcade.brennon.core.rank.CoreRankManager
import com.envarcade.brennon.core.auth.PlayerAuthManager
import com.envarcade.brennon.core.rank.LuckPermsHook
import com.envarcade.brennon.core.rank.RankUpdateSubscriber
import com.envarcade.brennon.core.scheduler.BrennonScheduler
import com.envarcade.brennon.core.server.CoreServerManager
import com.envarcade.brennon.core.server.ServerRegistryService
import com.envarcade.brennon.core.staff.ReportManager
import com.envarcade.brennon.core.network.CrossNetworkService
import com.envarcade.brennon.core.staff.StaffManager
import com.envarcade.brennon.core.stats.CoreStatsManager
import com.envarcade.brennon.core.ticket.CoreTicketManager
import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import com.envarcade.brennon.messaging.channel.Channels
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Main Brennon core bootstrap.
 *
 * This class initializes all core services and registers the API.
 * Platform-specific modules (Bukkit, Velocity) call this during startup.
 */
class Brennon(
    private val platform: Platform,
    private val dataFolder: File
) : BrennonAPI {

    lateinit var config: BrennonConfig
        private set
    lateinit var networkContext: NetworkContext
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var redisMessaging: RedisMessagingService
        private set

    // Core managers
    lateinit var coreEventBus: CoreEventBus
        private set
    lateinit var coreModuleManager: CoreModuleManager
        private set
    lateinit var coreRankManager: CoreRankManager
        private set
    lateinit var corePlayerManager: CorePlayerManager
        private set
    lateinit var coreEconomyManager: CoreEconomyManager
        private set
    lateinit var corePunishmentManager: CorePunishmentManager
        private set
    lateinit var coreServerManager: CoreServerManager
        private set
    lateinit var serverRegistryService: ServerRegistryService
        private set
    lateinit var scheduler: BrennonScheduler
        private set

    // Feature managers
    lateinit var coreChatManager: CoreChatManager
        private set
    lateinit var coreTicketManager: CoreTicketManager
        private set
    lateinit var coreStatsManager: CoreStatsManager
        private set
    lateinit var coreGuiManager: CoreGuiManager
        private set

    // Staff tools
    lateinit var staffManager: StaffManager
        private set
    lateinit var reportManager: ReportManager
        private set

    // Cross-network admin
    lateinit var crossNetworkService: CrossNetworkService
        private set

    // Player auth (web linking)
    lateinit var playerAuthManager: PlayerAuthManager
        private set

    // LuckPerms integration
    var luckPermsHook: LuckPermsHook? = null
        private set

    /** Platform callback to re-apply permissions when ranks change (fallback when LP not active) */
    var permissionRefreshCallback: ((UUID) -> Unit)? = null

    /** Platform hook: dispatches `lp editor` and returns the resulting URL. Set by Bukkit/Folia. */
    var luckPermsEditorProvider: (() -> CompletableFuture<String>)? = null

    // Commands
    lateinit var commandRegistry: CommandRegistry
        private set

    fun enable(preloadedConfig: BrennonConfig? = null) {
        val startTime = System.currentTimeMillis()

        println("========================================")
        println(" Brennon Network Core v${VERSION}")
        println(" Platform: ${platform.displayName}")
        println("========================================")

        // Load configuration
        config = preloadedConfig ?: ConfigLoader.loadBrennonConfig(dataFolder)
        println("[Brennon] Configuration loaded. Server: ${config.serverName} (${config.serverGroup})")

        // Create network context
        networkContext = NetworkContext(
            networkId = config.network.networkId,
            sharing = config.network.sharing
        )
        println("[Brennon] Network: ${config.network.displayName} (${networkContext.networkId})")

        // Initialize infrastructure
        databaseManager = DatabaseManager(config.database, networkContext)
        databaseManager.initialize()

        redisMessaging = RedisMessagingService(config.redis)
        redisMessaging.initialize()

        // Initialize core systems
        coreEventBus = CoreEventBus()
        coreModuleManager = CoreModuleManager()

        coreRankManager = CoreRankManager(databaseManager, redisMessaging, coreEventBus)
        coreRankManager.initialize().join()

        // Initialize LuckPerms hook (if LP is installed and config enabled, skip for standalone)
        if (config.luckperms.enabled && platform.type != Platform.PlatformType.STANDALONE) {
            try {
                val hook = LuckPermsHook(config.luckperms, coreRankManager, coreEventBus)
                if (hook.initialize()) {
                    luckPermsHook = hook
                    coreRankManager.luckPermsHook = hook
                }
            } catch (e: Exception) {
                println("[Brennon] LuckPerms initialization failed: ${e.message}")
            }
        }

        // Subscribe to LP editor requests (game servers relay `lp editor` command)
        if (config.luckperms.enabled && platform.type != Platform.PlatformType.STANDALONE) {
            redisMessaging.subscribe(Channels.LUCKPERMS_EDITOR_REQUEST) { _, message ->
                val json = JsonParser.parseString(message).asJsonObject
                val requestId = json.get("requestId").asString

                val provider = luckPermsEditorProvider
                if (provider == null || luckPermsHook?.isActive != true) return@subscribe

                // Use Redis SETNX to claim this request (only one server handles it)
                if (!redisMessaging.tryLock("lp:editor:lock:$requestId", 30)) return@subscribe

                provider.invoke().thenAccept { url ->
                    val response = JsonObject().apply {
                        addProperty("requestId", requestId)
                        addProperty("url", url)
                    }
                    redisMessaging.publish(Channels.LUCKPERMS_EDITOR_RESPONSE, response.toString())
                }.exceptionally { e ->
                    val response = JsonObject().apply {
                        addProperty("requestId", requestId)
                        addProperty("error", e.message ?: "Failed to generate editor URL")
                    }
                    redisMessaging.publish(Channels.LUCKPERMS_EDITOR_RESPONSE, response.toString())
                    null
                }
            }
        }

        corePlayerManager = CorePlayerManager(
            databaseManager, redisMessaging, coreRankManager, coreEventBus, config.serverName
        )

        // Subscribe to cross-server rank updates
        RankUpdateSubscriber(
            redisMessaging, corePlayerManager, coreRankManager, luckPermsHook
        ) { permissionRefreshCallback }.initialize()

        coreEconomyManager = CoreEconomyManager(
            databaseManager, corePlayerManager, redisMessaging, coreEventBus
        )

        // Network-scoped managers
        val punishmentNetworkId = networkContext.effectiveNetworkId(networkContext.sharing.punishments)
        corePunishmentManager = CorePunishmentManager(
            databaseManager, corePlayerManager, redisMessaging, coreEventBus, punishmentNetworkId
        )

        serverRegistryService = ServerRegistryService(redisMessaging, coreEventBus)
        serverRegistryService.initialize()

        coreServerManager = CoreServerManager(redisMessaging, config.serverName, config.serverGroup, networkContext.networkId)
        coreServerManager.registryService = serverRegistryService
        coreServerManager.initialize(sendHeartbeats = platform.type != Platform.PlatformType.STANDALONE)

        // Staff tools (reports are network-scoped)
        val reportNetworkId = networkContext.effectiveNetworkId(networkContext.sharing.reports)
        staffManager = StaffManager(redisMessaging, config.serverName)
        reportManager = ReportManager(redisMessaging, config.serverName, reportNetworkId)

        // Feature managers
        if (config.modules.chat) {
            val chatNetworkId = networkContext.effectiveNetworkId(networkContext.sharing.chat)
            coreChatManager = CoreChatManager(
                corePlayerManager, corePunishmentManager, redisMessaging, coreEventBus, config, chatNetworkId
            )
            coreChatManager.initialize()
        }

        if (config.modules.tickets) {
            val ticketNetworkId = networkContext.effectiveNetworkId(networkContext.sharing.tickets)
            coreTicketManager = CoreTicketManager(databaseManager, redisMessaging, coreEventBus, ticketNetworkId)
        }

        if (config.modules.stats) {
            coreStatsManager = CoreStatsManager(databaseManager, redisMessaging, coreEventBus)

            // Wire stats auto-tracking hooks
            corePunishmentManager.statsTracker = { uuid, statId ->
                coreStatsManager.incrementStat(uuid, statId, 1.0)
            }
            coreEconomyManager.statsTracker = { uuid, statId, amount ->
                coreStatsManager.incrementStat(uuid, statId, amount)
            }
            reportManager.statsTracker = { uuid, statId ->
                coreStatsManager.incrementStat(uuid, statId, 1.0)
            }
            if (config.modules.chat) {
                coreChatManager.statsTracker = { uuid, statId ->
                    coreStatsManager.incrementStat(uuid, statId, 1.0)
                }
            }
        }

        coreGuiManager = CoreGuiManager()

        // Player auth (web linking)
        playerAuthManager = PlayerAuthManager()

        // Cross-network admin service
        crossNetworkService = CrossNetworkService(databaseManager)

        // Register commands (skip for standalone — webserver uses HTTP routes)
        commandRegistry = CommandRegistry()
        if (platform.type != Platform.PlatformType.STANDALONE) {
            registerCommands()
        }

        // Start scheduler
        scheduler = BrennonScheduler(
            corePlayerManager,
            if (::coreStatsManager.isInitialized) coreStatsManager else null
        )
        scheduler.start()

        // Register the API
        BrennonProvider.register(this)

        val elapsed = System.currentTimeMillis() - startTime
        println("[Brennon] All systems online. Enabled in ${elapsed}ms.")
    }

    fun disable() {
        println("[Brennon] Shutting down...")

        luckPermsHook?.shutdown()

        if (::coreStatsManager.isInitialized) coreStatsManager.flushAll()
        if (::coreChatManager.isInitialized) coreChatManager.shutdown()

        scheduler.stop()
        coreServerManager.shutdown()
        serverRegistryService.shutdown()
        corePlayerManager.shutdown()
        coreModuleManager.disableAll()
        coreEventBus.clear()
        redisMessaging.shutdown()
        databaseManager.shutdown()

        BrennonProvider.unregister()
        println("[Brennon] Disabled. Goodbye!")
    }

    private fun registerCommands() {
        // Punishment commands
        commandRegistry.register(BanCommand(this))
        commandRegistry.register(UnbanCommand(this))
        commandRegistry.register(MuteCommand(this))
        commandRegistry.register(UnmuteCommand(this))
        commandRegistry.register(KickCommand(this))
        commandRegistry.register(WarnCommand(this))
        commandRegistry.register(HistoryCommand(this))

        // Rank commands
        commandRegistry.register(RankCommand(this))

        // Economy commands
        commandRegistry.register(EconomyCommand(this))

        // Server commands
        commandRegistry.register(ServerCommand(this))
        commandRegistry.register(ServerAdminCommand(this))
        commandRegistry.register(ServerGroupCommand(this))

        // Staff commands
        commandRegistry.register(StaffModeCommand(this, staffManager))
        commandRegistry.register(VanishCommand(this, staffManager))
        commandRegistry.register(StaffChatCommand(this))
        commandRegistry.register(StaffListCommand(this, staffManager))
        commandRegistry.register(ReportCommand(this, reportManager))
        commandRegistry.register(ReportsCommand(this, reportManager))

        // Link command (web dashboard account linking)
        commandRegistry.register(LinkCommand(this))

        // Chat commands
        if (config.modules.chat) {
            commandRegistry.register(ChannelCommand(this))
            commandRegistry.register(MsgCommand(this))
            commandRegistry.register(ReplyCommand(this))
            commandRegistry.register(ChatFilterCommand(this))
        }

        // Ticket commands
        if (config.modules.tickets) {
            commandRegistry.register(TicketCommand(this))
        }

        // Stats commands
        if (config.modules.stats) {
            commandRegistry.register(StatsCommand(this))
            commandRegistry.register(LeaderboardCommand(this))
        }

        println("[Brennon] Registered ${commandRegistry.getCommands().size} commands.")
    }

    // BrennonAPI
    override fun getPlayerManager(): PlayerManager = corePlayerManager
    override fun getRankManager(): RankManager = coreRankManager
    override fun getEconomyManager(): EconomyManager = coreEconomyManager
    override fun getPunishmentManager(): PunishmentManager = corePunishmentManager
    override fun getServerManager(): ServerManager = coreServerManager
    override fun getMessagingService(): MessagingService = redisMessaging
    override fun getModuleManager(): ModuleManager = coreModuleManager
    override fun getChatManager(): ChatManager = coreChatManager
    override fun getTicketManager(): TicketManager = coreTicketManager
    override fun getStatsManager(): StatsManager = coreStatsManager
    override fun getGuiManager(): GuiManager = coreGuiManager
    override fun getPlatform(): Platform = platform
    override fun getNetworkId(): String = networkContext.networkId

    fun getEventBus(): EventBus = coreEventBus

    companion object {
        const val VERSION = "2.0.0-SNAPSHOT"
    }
}
