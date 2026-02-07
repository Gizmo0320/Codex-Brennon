package com.envarcade.brennon.api.gui;

import java.util.UUID;

/**
 * Context passed to a GUI item click handler.
 */
public interface GuiClickContext {

    /** The player who clicked. */
    UUID getPlayer();

    /** The slot that was clicked. */
    int getSlot();

    /** The click type. */
    ClickType getClickType();

    /** Close the GUI after handling. */
    void closeGui();

    enum ClickType {
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        MIDDLE,
        DROP,
        OTHER
    }
}
