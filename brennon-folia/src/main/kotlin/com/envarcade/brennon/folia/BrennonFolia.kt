package com.envarcade.brennon.folia

import com.envarcade.brennon.api.Platform
import com.envarcade.brennon.folia.command.FoliaCommandBridge
import com.envarcade.brennon.folia.listener.FoliaChatListener
import com.envarcade.brennon.folia.listener.FoliaGuiListener
import com.envarcade.brennon.folia.listener.FoliaPlayerListener
import com.envarcade.brennon.folia.luckperms.EditorUrlCapture
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.messaging.channel.Channels
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * Brennon Network Core — Folia Plugin.
 *
 * Folia is a Paper fork that uses regionized multithreading.
 * Key differences from Paper:
 * - No global tick scheduler — must use region-aware schedulers
 * - Bukkit.getScheduler() is unsupported — use entity/region schedulers
 * - Most Bukkit API works, but scheduling must be adapted
 *
 * This plugin reuses Bukkit listeners but uses Folia's
 * RegionScheduler and AsyncScheduler for task scheduling.
 */
class BrennonFolia : JavaPlugin() {

    lateinit var brennon: Brennon
        private set

    override fun onEnable() {
        // Verify we're actually on Folia
        if (!isFolia()) {
            logger.severe("[Brennon] This plugin requires Folia! Use brennon-bukkit for Paper/Spigot.")
            server.pluginManager.disablePlugin(this)
            return
        }

        brennon = Brennon(Platform.FOLIA, dataFolder)
        brennon.enable()

        // Platform hooks
        brennon.corePlayerManager.messageSender = { uuid, component ->
            server.getPlayer(uuid)?.sendMessage(component)
        }

        // Permission refresh callback (fallback when LuckPerms not active)
        brennon.permissionRefreshCallback = refreshCallback@{ uuid ->
            val player = server.getPlayer(uuid) ?: return@refreshCallback
            val networkPlayer = brennon.corePlayerManager.getCachedPlayer(uuid) ?: return@refreshCallback

            // Remove existing attachments and re-apply
            for (info in player.effectivePermissions) {
                if (info.attachment != null && info.attachment!!.plugin?.name == "Brennon") {
                    player.removeAttachment(info.attachment!!)
                }
            }

            val attachment = player.addAttachment(this)
            val permissions = networkPlayer.getRanks()
                .flatMap { it.permissions }
                .toSet()

            for (perm in permissions) {
                if (perm.startsWith("-")) {
                    attachment.setPermission(perm.substring(1), false)
                } else {
                    attachment.setPermission(perm, true)
                }
            }

            player.recalculatePermissions()
        }
        brennon.coreServerManager.localPlayerCountProvider = { server.onlinePlayers.size }
        brennon.coreServerManager.localHostProvider = { brennon.config.serverHost.ifBlank { server.ip } }
        brennon.coreServerManager.localPortProvider = { brennon.config.serverPort }

        // Set chat manager hooks
        if (brennon.config.modules.chat) {
            brennon.coreChatManager.localMessageSender = { uuid, component ->
                server.getPlayer(uuid)?.sendMessage(component)
            }
        }

        // Set GUI platform hooks (same as Bukkit — inventory API is compatible)
        brennon.coreGuiManager.guiOpener = openGui@{ uuid, gui ->
            val player = server.getPlayer(uuid) ?: return@openGui
            val inventory = Bukkit.createInventory(null, gui.rows * 9, gui.title)

            for ((slot, item) in gui.items) {
                val material = Material.matchMaterial(item.material) ?: Material.STONE
                val stack = ItemStack(material, item.amount)
                val meta = stack.itemMeta
                if (meta != null) {
                    meta.displayName(item.displayName)
                    meta.lore(item.lore)
                    if (item.customModelData >= 0) {
                        meta.setCustomModelData(item.customModelData)
                    }
                    if (item.isGlowing) {
                        meta.addEnchant(Enchantment.DURABILITY, 1, true)
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                    stack.itemMeta = meta
                }
                inventory.setItem(slot, stack)
            }

            player.openInventory(inventory)
        }

        brennon.coreGuiManager.guiCloser = { uuid ->
            server.getPlayer(uuid)?.closeInventory()
        }

        // LuckPerms editor provider — dispatches `lp editor` and captures the URL
        brennon.luckPermsEditorProvider = {
            val future = CompletableFuture<String>()
            server.globalRegionScheduler.run(this) { _ ->
                val (proxy, urlFuture) = EditorUrlCapture.create(Bukkit.getConsoleSender())
                Bukkit.dispatchCommand(proxy, "lp editor")
                urlFuture.orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept { future.complete(it) }
                    .exceptionally { e -> future.completeExceptionally(e); null }
            }
            future
        }

        // Subscribe to remote kick requests (Folia: use entity scheduler)
        brennon.redisMessaging.subscribe(Channels.PLAYER_KICK) { _, message ->
            try {
                val json = JsonParser.parseString(message).asJsonObject
                val uuid = java.util.UUID.fromString(json.get("uuid").asString)
                val reason = json.get("reason")?.asString ?: "Kicked by admin"
                val player = server.getPlayer(uuid)
                if (player != null) {
                    player.scheduler.run(this, { _ ->
                        player.kick(net.kyori.adventure.text.Component.text(reason))
                    }, null)
                }
            } catch (e: Exception) {
                logger.warning("[Brennon] Failed to process kick request: ${e.message}")
            }
        }

        // Register listeners
        server.pluginManager.registerEvents(FoliaPlayerListener(brennon), this)
        server.pluginManager.registerEvents(FoliaChatListener(brennon), this)
        server.pluginManager.registerEvents(FoliaGuiListener(brennon), this)

        // Register commands
        FoliaCommandBridge(brennon, this).registerAll()

        logger.info("[Brennon] Folia plugin loaded. Region-aware scheduling active.")
    }

    override fun onDisable() {
        brennon.disable()
        logger.info("[Brennon] Folia plugin unloaded.")
    }

    /**
     * Detects if the server is running Folia by checking for the
     * RegionScheduler class which only exists in Folia.
     */
    private fun isFolia(): Boolean {
        return try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
}
