package com.envarcade.brennon.core.gui

import com.envarcade.brennon.api.gui.GuiClickContext
import com.envarcade.brennon.api.gui.GuiItem
import net.kyori.adventure.text.Component
import java.util.function.Consumer

class CoreGuiItem(
    private val material: String,
    private val displayName: Component,
    private val lore: List<Component>,
    private val amount: Int,
    private val glowing: Boolean,
    private val customModelData: Int,
    private val clickHandler: Consumer<GuiClickContext>?
) : GuiItem {
    override fun getMaterial(): String = material
    override fun getDisplayName(): Component = displayName
    override fun getLore(): List<Component> = lore
    override fun getAmount(): Int = amount
    override fun isGlowing(): Boolean = glowing
    override fun getCustomModelData(): Int = customModelData
    override fun getClickHandler(): Consumer<GuiClickContext>? = clickHandler
}
