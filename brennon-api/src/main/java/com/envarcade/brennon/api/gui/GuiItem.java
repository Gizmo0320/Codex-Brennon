package com.envarcade.brennon.api.gui;

import net.kyori.adventure.text.Component;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a single item within a BrennonGui.
 */
public interface GuiItem {

    /** Material/item type ID (e.g., "minecraft:diamond_sword"). */
    String getMaterial();

    /** Display name. */
    Component getDisplayName();

    /** Lore lines. */
    List<Component> getLore();

    /** Stack amount. */
    int getAmount();

    /** Whether this item has an enchant glow. */
    boolean isGlowing();

    /** Custom model data value, or -1 for none. */
    int getCustomModelData();

    /** The click handler for this item. */
    Consumer<GuiClickContext> getClickHandler();
}
