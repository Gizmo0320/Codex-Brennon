package com.envarcade.brennon.database.repository

import com.envarcade.brennon.common.model.PlayerData
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface PlayerRepository {
    fun findByUuid(uuid: UUID): CompletableFuture<PlayerData?>
    fun findByName(name: String): CompletableFuture<PlayerData?>
    fun save(player: PlayerData): CompletableFuture<Void>
    fun delete(uuid: UUID): CompletableFuture<Void>
    fun exists(uuid: UUID): CompletableFuture<Boolean>
}
