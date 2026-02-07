package com.envarcade.brennon.api.stats;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Tracks and queries player statistics across the network.
 */
public interface StatsManager {

    /** Gets a specific stat value for a player. */
    CompletableFuture<Double> getStat(UUID player, String statId);

    /** Gets all stats for a player. */
    CompletableFuture<Map<String, Double>> getAllStats(UUID player);

    /** Increments a stat by a given amount. */
    CompletableFuture<Void> incrementStat(UUID player, String statId, double amount);

    /** Sets a stat to a specific value. */
    CompletableFuture<Void> setStat(UUID player, String statId, double value);

    /** Gets the top N players for a stat (leaderboard). */
    CompletableFuture<Map<UUID, Double>> getLeaderboard(String statId, int limit);

    /** Gets a player's rank on a leaderboard. */
    CompletableFuture<Integer> getLeaderboardPosition(UUID player, String statId);

    /** Resets a stat for a player. */
    CompletableFuture<Void> resetStat(UUID player, String statId);

    /** Resets all stats for a player. */
    CompletableFuture<Void> resetAllStats(UUID player);
}
