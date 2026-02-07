package com.envarcade.brennon.database.repository

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface StatsRepository {
    fun getStat(uuid: UUID, statId: String): CompletableFuture<Double>
    fun getAllStats(uuid: UUID): CompletableFuture<Map<String, Double>>
    fun setStat(uuid: UUID, statId: String, value: Double): CompletableFuture<Void>
    fun incrementStat(uuid: UUID, statId: String, amount: Double): CompletableFuture<Void>
    fun getLeaderboard(statId: String, limit: Int): CompletableFuture<Map<UUID, Double>>
    fun getLeaderboardPosition(uuid: UUID, statId: String): CompletableFuture<Int>
    fun resetStat(uuid: UUID, statId: String): CompletableFuture<Void>
    fun resetAllStats(uuid: UUID): CompletableFuture<Void>
}
