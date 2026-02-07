package com.envarcade.brennon.core.gui

import com.envarcade.brennon.api.gui.GuiClickContext
import java.util.UUID

class CoreGuiClickContext(
    private val player: UUID,
    private val slot: Int,
    private val clickType: GuiClickContext.ClickType,
    private val guiManager: CoreGuiManager
) : GuiClickContext {
    override fun getPlayer(): UUID = player
    override fun getSlot(): Int = slot
    override fun getClickType(): GuiClickContext.ClickType = clickType
    override fun closeGui() {
        guiManager.closeGui(player)
    }
}
