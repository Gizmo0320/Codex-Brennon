package com.envarcade.brennon.api.rank;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface RankManager {

    Optional<Rank> getRank(String id);

    Collection<Rank> getRanks();

    Rank getDefaultRank();

    CompletableFuture<Void> setPlayerRank(UUID uuid, String rankId);

    CompletableFuture<Void> addPlayerRank(UUID uuid, String rankId);

    CompletableFuture<Void> removePlayerRank(UUID uuid, String rankId);
}
