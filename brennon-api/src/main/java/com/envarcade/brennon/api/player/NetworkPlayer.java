package com.envarcade.brennon.api.player;

import com.envarcade.brennon.api.rank.Rank;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface NetworkPlayer {

    UUID getUniqueId();

    String getName();

    Component getDisplayName();

    Rank getRank();

    Set<Rank> getRanks();

    boolean hasPermission(String permission);

    String getCurrentServer();

    Instant getFirstJoin();

    Instant getLastSeen();

    boolean isOnline();

    void sendMessage(Component message);
}
