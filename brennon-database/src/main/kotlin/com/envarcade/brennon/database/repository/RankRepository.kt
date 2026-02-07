package com.envarcade.brennon.database.repository

import com.envarcade.brennon.common.model.RankData
import java.util.concurrent.CompletableFuture

interface RankRepository {
    fun findById(id: String): CompletableFuture<RankData?>
    fun findAll(): CompletableFuture<List<RankData>>
    fun findDefault(): CompletableFuture<RankData?>
    fun save(rank: RankData): CompletableFuture<Void>
    fun delete(id: String): CompletableFuture<Void>
    fun exists(id: String): CompletableFuture<Boolean>
}
