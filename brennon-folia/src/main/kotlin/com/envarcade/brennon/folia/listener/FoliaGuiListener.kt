package com.envarcade.brennon.folia.listener

import com.envarcade.brennon.api.gui.GuiClickContext
import com.envarcade.brennon.core.Brennon
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent

/**
 * Folia listener for GUI inventory interactions.
 *
 * Maps Bukkit inventory events to the Brennon GUI framework.
 */
class FoliaGuiListener(private val brennon: Brennon) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        val uuid = player.uniqueId

        if (!brennon.coreGuiManager.hasOpenGui(uuid)) return

        val clickType = when (event.click) {
            ClickType.LEFT -> GuiClickContext.ClickType.LEFT
            ClickType.RIGHT -> GuiClickContext.ClickType.RIGHT
            ClickType.SHIFT_LEFT -> GuiClickContext.ClickType.SHIFT_LEFT
            ClickType.SHIFT_RIGHT -> GuiClickContext.ClickType.SHIFT_RIGHT
            ClickType.MIDDLE -> GuiClickContext.ClickType.MIDDLE
            ClickType.DROP, ClickType.CONTROL_DROP -> GuiClickContext.ClickType.DROP
            else -> GuiClickContext.ClickType.OTHER
        }

        val handled = brennon.coreGuiManager.handleClick(uuid, event.rawSlot, clickType)
        if (handled) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onClose(event: InventoryCloseEvent) {
        brennon.coreGuiManager.handleClose(event.player.uniqueId)
    }
}
