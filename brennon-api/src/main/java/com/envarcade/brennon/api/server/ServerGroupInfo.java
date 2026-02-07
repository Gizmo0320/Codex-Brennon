package com.envarcade.brennon.api.server;

/**
 * Represents a server group with metadata.
 *
 * Groups organize servers into logical categories (lobby, survival, pvp, etc.)
 * and define group-level properties.
 */
public interface ServerGroupInfo {

    String getId();

    String getDisplayName();

    int getJoinPriority();

    boolean isRestricted();

    String getPermission();

    boolean isFallback();

    int getMaxPlayers();
}
