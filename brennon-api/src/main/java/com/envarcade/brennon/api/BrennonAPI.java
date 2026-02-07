package com.envarcade.brennon.api;

import com.envarcade.brennon.api.chat.ChatManager;
import com.envarcade.brennon.api.economy.EconomyManager;
import com.envarcade.brennon.api.gui.GuiManager;
import com.envarcade.brennon.api.messaging.MessagingService;
import com.envarcade.brennon.api.module.ModuleManager;
import com.envarcade.brennon.api.player.PlayerManager;
import com.envarcade.brennon.api.punishment.PunishmentManager;
import com.envarcade.brennon.api.rank.RankManager;
import com.envarcade.brennon.api.server.ServerManager;
import com.envarcade.brennon.api.stats.StatsManager;
import com.envarcade.brennon.api.ticket.TicketManager;

/**
 * Main Brennon Network Core API.
 *
 * Access this via {@link BrennonProvider#get()}.
 *
 * Provides a unified API surface for all Brennon systems, accessible
 * from any platform (Paper, Folia, Forge, NeoForge, Sponge, Velocity).
 */
public interface BrennonAPI {

    // Core managers
    PlayerManager getPlayerManager();
    RankManager getRankManager();
    EconomyManager getEconomyManager();
    PunishmentManager getPunishmentManager();
    ServerManager getServerManager();
    MessagingService getMessagingService();
    ModuleManager getModuleManager();

    // Feature managers
    ChatManager getChatManager();
    TicketManager getTicketManager();
    StatsManager getStatsManager();
    GuiManager getGuiManager();

    // Platform info
    Platform getPlatform();

    // Network info
    String getNetworkId();
}
