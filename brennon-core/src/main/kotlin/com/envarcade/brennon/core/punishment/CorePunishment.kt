package com.envarcade.brennon.core.punishment

import com.envarcade.brennon.api.punishment.Punishment
import com.envarcade.brennon.common.model.PunishmentData
import java.time.Instant
import java.util.UUID

/**
 * Core implementation of the Punishment interface backed by PunishmentData.
 */
class CorePunishment(private val data: PunishmentData) : Punishment {

    override fun getId(): String = data.id
    override fun getTarget(): UUID = data.target
    override fun getIssuer(): UUID? = data.issuer
    override fun getType() = data.type
    override fun getReason(): String = data.reason
    override fun getIssuedAt(): Instant = data.issuedAt
    override fun getExpiresAt(): Instant? = data.expiresAt
    override fun isActive(): Boolean = data.isEffectivelyActive()
    override fun isPermanent(): Boolean = data.expiresAt == null
    override fun isRevoked(): Boolean = data.revokedBy != null
    override fun getRevokedBy(): UUID? = data.revokedBy

    fun getData(): PunishmentData = data
}
