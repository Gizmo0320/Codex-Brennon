package com.envarcade.brennon.common.model

import com.envarcade.brennon.api.punishment.PunishmentType
import java.time.Instant
import java.util.UUID

data class PunishmentData(
    val id: String,
    val target: UUID,
    val issuer: UUID?,
    val type: PunishmentType,
    val reason: String,
    val issuedAt: Instant = Instant.now(),
    val expiresAt: Instant? = null,
    var active: Boolean = true,
    var revokedBy: UUID? = null,
    var revokedAt: Instant? = null,
    var revokeReason: String? = null,
    val networkId: String? = null,
    val targetIp: String? = null
) {
    fun hasExpired(): Boolean {
        if (expiresAt == null) return false
        return Instant.now().isAfter(expiresAt)
    }

    fun isEffectivelyActive(): Boolean = active && !hasExpired()
}
