package com.envarcade.brennon.bukkit

import com.envarcade.brennon.api.Platform
import com.envarcade.brennon.bukkit.command.BukkitCommandBridge
import com.envarcade.brennon.bukkit.listener.BukkitChatListener
import com.envarcade.brennon.bukkit.listener.BukkitGuiListener
import com.envarcade.brennon.bukkit.listener.BukkitPlayerListener
import com.envarcade.brennon.bukkit.luckperms.EditorUrlCapture
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.messaging.channel.Channels
import com.google.gson.JsonParser
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * Brennon Network Core — Paper/Spigot Server Plugin.
 */
class BrennonBukkit : JavaPlugin() {

    lateinit var brennon: Brennon
        private set

    override fun onEnable() {
        brennon = Brennon(Platform.PAPER, dataFolder)
        brennon.enable()

        // Set platform-specific hooks
        brennon.corePlayerManager.messageSender = { uuid, component ->
            server.getPlayer(uuid)?.sendMessage(component)
        }

        // Permission refresh callback (fallback when LuckPerms not active)
        brennon.permissionRefreshCallback = refreshCallback@{ uuid ->
            val player = server.getPlayer(uuid) ?: return@refreshCallback
            val networkPlayer = brennon.corePlayerManager.getCachedPlayer(uuid) ?: return@refreshCallback

            // Remove existing attachments and re-apply
            for (info in player.effectivePermissions) {
                val attachment = info.attachment ?: continue
                if (attachment.plugin.name == "Brennon") {
                    player.removeAttachment(attachment)
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

        // Set GUI platform hooks
        brennon.coreGuiManager.guiOpener = openGui@{ uuid, gui ->
            val player = server.getPlayer(uuid) ?: return@openGui
            val inventory = Bukkit.createInventory(null, gui.rows * 9, Component.text(gui.title))

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
            Bukkit.getScheduler().runTask(this, Runnable {
                val (proxy, urlFuture) = EditorUrlCapture.create(Bukkit.getConsoleSender())
                Bukkit.dispatchCommand(proxy, "lp editor")
                urlFuture.orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept { future.complete(it) }
                    .exceptionally { e -> future.completeExceptionally(e); null }
            })
            future
        }

        // Subscribe to remote kick requests
        brennon.redisMessaging.subscribe(Channels.PLAYER_KICK) { _, message ->
            try {
                val json = JsonParser.parseString(message).asJsonObject
                val uuid = java.util.UUID.fromString(json.get("uuid").asString)
                val reason = json.get("reason")?.asString ?: "Kicked by admin"
                val player = server.getPlayer(uuid)
                if (player != null) {
                    Bukkit.getScheduler().runTask(this, Runnable {
                        player.kick(net.kyori.adventure.text.Component.text(reason))
                    })
                }
            } catch (e: Exception) {
                logger.warning("[Brennon] Failed to process kick request: ${e.message}")
            }
        }

        // Subscribe to staff alerts (punishment notifications)
        brennon.redisMessaging.subscribe(Channels.STAFF_ALERT) { _, message ->
            try {
                val json = JsonParser.parseString(message).asJsonObject
                if (json.get("type")?.asString == "punishment") {
                    val action = json.get("action")?.asString ?: return@subscribe
                    val punishmentType = json.get("punishmentType")?.asString ?: return@subscribe
                    val target = json.get("target")?.asString ?: return@subscribe
                    val targetName = brennon.corePlayerManager.getCachedPlayer(
                        java.util.UUID.fromString(target)
                    )?.getName() ?: target

                    val alertMsg = if (action == "issued") {
                        val issuer = json.get("issuer")?.asString ?: "CONSOLE"
                        val reason = json.get("reason")?.asString ?: ""
                        "\u00a7e[Staff] \u00a7c$punishmentType \u00a7eissued to \u00a7f$targetName \u00a7eby \u00a7f$issuer\u00a7e: \u00a77$reason"
                    } else {
                        val revokedBy = json.get("revokedBy")?.asString ?: "CONSOLE"
                        "\u00a7e[Staff] \u00a7a$punishmentType \u00a7erevoked for \u00a7f$targetName \u00a7eby \u00a7f$revokedBy"
                    }

                    for (player in server.onlinePlayers) {
                        if (player.hasPermission("brennon.staff.alerts")) {
                            player.sendMessage(net.kyori.adventure.text.Component.text(alertMsg))
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Register listeners
        server.pluginManager.registerEvents(BukkitPlayerListener(brennon), this)
        server.pluginManager.registerEvents(BukkitChatListener(brennon), this)
        server.pluginManager.registerEvents(BukkitGuiListener(brennon), this)

        // Register commands
        BukkitCommandBridge(brennon, this).registerAll()

        logger.info("[Brennon] Bukkit plugin loaded. ${brennon.commandRegistry.getCommands().size} commands registered.")
    }

    override fun onDisable() {
        brennon.disable()
        logger.info("[Brennon] Bukkit plugin unloaded.")
    }
}
