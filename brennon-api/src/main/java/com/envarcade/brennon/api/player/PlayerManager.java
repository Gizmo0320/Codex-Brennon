package com.envarcade.brennon.api.player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerManager {

    CompletableFuture<Optional<NetworkPlayer>> getPlayer(UUID uuid);

    CompletableFuture<Optional<NetworkPlayer>> getPlayer(String name);

    NetworkPlayer getOnlinePlayer(UUID uuid);

    boolean isOnline(UUID uuid);

    int getOnlineCount();
}
