package com.envarcade.brennon.database.repository

import com.envarcade.brennon.api.punishment.PunishmentType
import com.envarcade.brennon.common.model.PunishmentData
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface PunishmentRepository {
    fun findById(id: String): CompletableFuture<PunishmentData?>
    fun findByTarget(uuid: UUID): CompletableFuture<List<PunishmentData>>
    fun findActiveByTarget(uuid: UUID): CompletableFuture<List<PunishmentData>>
    fun findActiveByTargetAndType(uuid: UUID, type: PunishmentType): CompletableFuture<List<PunishmentData>>
    fun findActiveByIp(ip: String): CompletableFuture<List<PunishmentData>>
    fun findAllByType(type: PunishmentType, limit: Int, offset: Int): CompletableFuture<List<PunishmentData>>
    fun countByType(type: PunishmentType): CompletableFuture<Int>
    fun save(punishment: PunishmentData): CompletableFuture<Void>
    fun delete(id: String): CompletableFuture<Void>
}
