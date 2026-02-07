package com.envarcade.brennon.core.gui

import com.envarcade.brennon.api.gui.BrennonGui
import com.envarcade.brennon.api.gui.GuiBuilder
import com.envarcade.brennon.api.gui.GuiClickContext
import com.envarcade.brennon.api.gui.GuiManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CoreGuiManager : GuiManager {

    private val openGuis = ConcurrentHashMap<UUID, BrennonGui>()

    /** Platform hook — set by platform module to open an inventory for a player. */
    var guiOpener: ((UUID, BrennonGui) -> Unit)? = null

    /** Platform hook — set by platform module to close an inventory for a player. */
    var guiCloser: ((UUID) -> Unit)? = null

    override fun createGui(title: String, rows: Int): GuiBuilder {
        return CoreGuiBuilder(title, rows)
    }

    override fun openGui(player: UUID, gui: BrennonGui) {
        openGuis[player] = gui
        guiOpener?.invoke(player, gui)
    }

    override fun closeGui(player: UUID) {
        openGuis.remove(player)
        guiCloser?.invoke(player)
    }

    override fun hasOpenGui(player: UUID): Boolean {
        return openGuis.containsKey(player)
    }

    /**
     * Called by platform listeners when a player clicks in a GUI.
     * Returns true if the click was handled by a Brennon GUI.
     */
    fun handleClick(player: UUID, slot: Int, clickType: GuiClickContext.ClickType): Boolean {
        val gui = openGuis[player] ?: return false
        val item = gui.getItem(slot) ?: return gui.isCancelClicks()
        val context = CoreGuiClickContext(player, slot, clickType, this)
        item.clickHandler?.accept(context)
        return gui.isCancelClicks()
    }

    /**
     * Called by platform listeners when a player closes an inventory.
     */
    fun handleClose(player: UUID) {
        openGuis.remove(player)
    }
}
