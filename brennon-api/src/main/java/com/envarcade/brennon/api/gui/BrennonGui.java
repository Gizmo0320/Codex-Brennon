package com.envarcade.brennon.api.gui;

import java.util.Map;

/**
 * Represents a Brennon GUI (inventory menu) definition.
 */
public interface BrennonGui {

    /** The title of the GUI. */
    String getTitle();

    /** Number of rows (1-6). */
    int getRows();

    /** Gets the item at a specific slot. */
    GuiItem getItem(int slot);

    /** Gets all items mapped by slot. */
    Map<Integer, GuiItem> getItems();

    /** Whether to prevent item removal from this GUI. */
    boolean isCancelClicks();
}
