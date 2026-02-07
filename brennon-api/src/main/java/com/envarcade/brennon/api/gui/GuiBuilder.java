package com.envarcade.brennon.api.gui;

import net.kyori.adventure.text.Component;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for constructing BrennonGui instances.
 */
public interface GuiBuilder {

    /** Sets an item at a specific slot. */
    GuiBuilder setItem(int slot, GuiItem item);

    /** Sets an item using the item builder. */
    GuiBuilder setItem(int slot, Consumer<GuiItemBuilder> itemBuilder);

    /** Fills empty slots with a given item (typically glass panes). */
    GuiBuilder fillEmpty(GuiItem item);

    /** Fills a border around the GUI. */
    GuiBuilder fillBorder(GuiItem item);

    /** Fills a row with an item. */
    GuiBuilder fillRow(int row, GuiItem item);

    /** Sets whether clicks are cancelled (items can't be taken). */
    GuiBuilder cancelClicks(boolean cancel);

    /** Builds the GUI. */
    BrennonGui build();

    /**
     * Builder for creating GuiItems.
     */
    interface GuiItemBuilder {
        GuiItemBuilder material(String material);
        GuiItemBuilder displayName(Component name);
        GuiItemBuilder lore(List<Component> lore);
        GuiItemBuilder amount(int amount);
        GuiItemBuilder glowing(boolean glow);
        GuiItemBuilder customModelData(int data);
        GuiItemBuilder onClick(Consumer<GuiClickContext> handler);
        GuiItem build();
    }
}
