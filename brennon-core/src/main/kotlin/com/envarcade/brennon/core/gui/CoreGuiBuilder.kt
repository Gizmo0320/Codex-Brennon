package com.envarcade.brennon.core.gui

import com.envarcade.brennon.api.gui.BrennonGui
import com.envarcade.brennon.api.gui.GuiBuilder
import com.envarcade.brennon.api.gui.GuiClickContext
import com.envarcade.brennon.api.gui.GuiItem
import net.kyori.adventure.text.Component
import java.util.function.Consumer

class CoreGuiBuilder(
    private val title: String,
    private val rows: Int
) : GuiBuilder {

    private val items = mutableMapOf<Int, GuiItem>()
    private var cancelClicks = true

    override fun setItem(slot: Int, item: GuiItem): GuiBuilder {
        items[slot] = item
        return this
    }

    override fun setItem(slot: Int, itemBuilder: Consumer<GuiBuilder.GuiItemBuilder>): GuiBuilder {
        val builder = CoreGuiItemBuilder()
        itemBuilder.accept(builder)
        items[slot] = builder.build()
        return this
    }

    override fun fillEmpty(item: GuiItem): GuiBuilder {
        val totalSlots = rows * 9
        for (i in 0 until totalSlots) {
            if (!items.containsKey(i)) {
                items[i] = item
            }
        }
        return this
    }

    override fun fillBorder(item: GuiItem): GuiBuilder {
        val totalSlots = rows * 9
        for (i in 0 until totalSlots) {
            val row = i / 9
            val col = i % 9
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                items[i] = item
            }
        }
        return this
    }

    override fun fillRow(row: Int, item: GuiItem): GuiBuilder {
        val startSlot = row * 9
        for (i in startSlot until startSlot + 9) {
            items[i] = item
        }
        return this
    }

    override fun cancelClicks(cancel: Boolean): GuiBuilder {
        cancelClicks = cancel
        return this
    }

    override fun build(): BrennonGui {
        return CoreBrennonGui(title, rows, items.toMap(), cancelClicks)
    }

    class CoreGuiItemBuilder : GuiBuilder.GuiItemBuilder {
        private var material: String = "minecraft:stone"
        private var displayName: Component = Component.empty()
        private var lore: List<Component> = emptyList()
        private var amount: Int = 1
        private var glowing: Boolean = false
        private var customModelData: Int = -1
        private var clickHandler: Consumer<GuiClickContext>? = null

        override fun material(material: String): GuiBuilder.GuiItemBuilder { this.material = material; return this }
        override fun displayName(name: Component): GuiBuilder.GuiItemBuilder { this.displayName = name; return this }
        override fun lore(lore: List<Component>): GuiBuilder.GuiItemBuilder { this.lore = lore; return this }
        override fun amount(amount: Int): GuiBuilder.GuiItemBuilder { this.amount = amount; return this }
        override fun glowing(glow: Boolean): GuiBuilder.GuiItemBuilder { this.glowing = glow; return this }
        override fun customModelData(data: Int): GuiBuilder.GuiItemBuilder { this.customModelData = data; return this }
        override fun onClick(handler: Consumer<GuiClickContext>): GuiBuilder.GuiItemBuilder { this.clickHandler = handler; return this }

        override fun build(): GuiItem {
            return CoreGuiItem(material, displayName, lore, amount, glowing, customModelData, clickHandler)
        }
    }
}
