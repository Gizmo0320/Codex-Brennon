package com.envarcade.brennon.core.auth

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the MC account → web dashboard linking flow.
 *
 * In-game: player runs /link → gets a 6-digit code (5-min TTL).
 * Web: player enters code → verifies and gets a JWT.
 */
class PlayerAuthManager {

    private val pendingLinks = ConcurrentHashMap<String, PendingLink>()

    /** Generates a random 6-digit link code for the given player. */
    fun generateLinkCode(uuid: UUID, playerName: String): String {
        // Remove any existing code for this player
        pendingLinks.entries.removeIf { it.value.uuid == uuid }

        val code = (100000..999999).random().toString()
        pendingLinks[code] = PendingLink(uuid, playerName, System.currentTimeMillis())
        return code
    }

    /** Verifies a link code. Returns the pending link if valid and not expired, null otherwise. */
    fun verifyLinkCode(code: String): PendingLink? {
        val link = pendingLinks.remove(code) ?: return null
        if (System.currentTimeMillis() - link.createdAt > CODE_TTL_MS) {
            return null // Expired
        }
        return link
    }

    /** Removes expired codes. Should be called periodically. */
    fun cleanupExpiredCodes() {
        val now = System.currentTimeMillis()
        pendingLinks.entries.removeIf { now - it.value.createdAt > CODE_TTL_MS }
    }

    data class PendingLink(
        val uuid: UUID,
        val playerName: String,
        val createdAt: Long
    )

    companion object {
        private const val CODE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }
}
