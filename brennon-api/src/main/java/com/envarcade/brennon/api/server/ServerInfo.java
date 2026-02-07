package com.envarcade.brennon.api.server;

public interface ServerInfo {

    String getName();

    String getGroup();

    int getPlayerCount();

    int getMaxPlayers();

    boolean isOnline();

    String getMotd();

    boolean isWhitelisted();
}
