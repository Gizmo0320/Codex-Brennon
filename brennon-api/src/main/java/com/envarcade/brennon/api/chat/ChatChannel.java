package com.envarcade.brennon.api.chat;

import java.util.Set;

/**
 * Represents a chat channel that players can join and send messages to.
 */
public interface ChatChannel {

    /** Unique channel ID (e.g., "global", "staff", "trade"). */
    String getId();

    /** Display name shown in chat. */
    String getDisplayName();

    /** MiniMessage format string for chat messages. */
    String getFormat();

    /** Permission required to see and send in this channel. */
    String getPermission();

    /** Permission required to send in this channel (optional stricter send perm). */
    String getSendPermission();

    /** Whether this channel broadcasts across all servers. */
    boolean isCrossServer();

    /** Whether this channel is the default channel. */
    boolean isDefault();

    /** Short prefix/alias for quick switching (e.g., "g" for global). */
    String getShortcut();

    /** Radius in blocks for local chat (-1 for no radius limit). */
    int getRadius();

    /** Server groups this channel is limited to (empty = all). */
    Set<String> getServerGroups();
}
