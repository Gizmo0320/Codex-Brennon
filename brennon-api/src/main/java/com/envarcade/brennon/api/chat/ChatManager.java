package com.envarcade.brennon.api.chat;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages cross-server chat channels, formatting, and filters.
 */
public interface ChatManager {

    /** Gets a chat channel by ID. */
    Optional<ChatChannel> getChannel(String id);

    /** Gets all registered channels. */
    Collection<ChatChannel> getChannels();

    /** Gets the default global channel. */
    ChatChannel getDefaultChannel();

    /** Gets the channel a player is currently focused on. */
    ChatChannel getPlayerChannel(UUID player);

    /** Sets the channel a player is focused on. */
    void setPlayerChannel(UUID player, String channelId);

    /** Sends a message to a channel. */
    void sendMessage(UUID sender, String senderName, String channelId, String message);

    /** Sends a private message between two players. */
    void sendPrivateMessage(UUID sender, String senderName, UUID recipient, String message);

    /** Checks if a player is muted in a specific channel. */
    boolean isMutedInChannel(UUID player, String channelId);

    /** Toggles a player's subscription to a channel. */
    void toggleChannelSubscription(UUID player, String channelId);
}
