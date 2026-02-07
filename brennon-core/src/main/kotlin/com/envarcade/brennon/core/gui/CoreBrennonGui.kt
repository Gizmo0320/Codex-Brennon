package com.envarcade.brennon.core.gui

import com.envarcade.brennon.api.gui.BrennonGui
import com.envarcade.brennon.api.gui.GuiItem

class CoreBrennonGui(
    private val title: String,
    private val rows: Int,
    private val items: Map<Int, GuiItem>,
    private val cancelClicks: Boolean
) : BrennonGui {
    override fun getTitle(): String = title
    override fun getRows(): Int = rows
    override fun getItem(slot: Int): GuiItem? = items[slot]
    override fun getItems(): Map<Int, GuiItem> = items
    override fun isCancelClicks(): Boolean = cancelClicks
}
