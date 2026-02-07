package com.envarcade.brennon.api.gui;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cross-platform GUI (inventory menu) framework.
 *
 * Provides a builder-style API that works across Paper, Folia, Sponge,
 * and Forge platforms. Each platform module translates BrennonGUI
 * definitions into native inventory implementations.
 */
public interface GuiManager {

    /** Creates a new GUI builder. */
    GuiBuilder createGui(String title, int rows);

    /** Opens a GUI for a player. */
    void openGui(UUID player, BrennonGui gui);

    /** Closes a player's current GUI. */
    void closeGui(UUID player);

    /** Checks if a player has a GUI open. */
    boolean hasOpenGui(UUID player);
}
