package com.envarcade.brennon.core.punishment

import com.envarcade.brennon.api.punishment.Punishment
import com.envarcade.brennon.api.punishment.PunishmentManager
import com.envarcade.brennon.api.punishment.PunishmentType
import com.envarcade.brennon.common.model.PunishmentData
import com.envarcade.brennon.common.util.UUIDUtil
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.PunishmentIssuedEvent
import com.envarcade.brennon.core.event.PunishmentRevokedEvent
import com.envarcade.brennon.core.player.CorePlayerManager
import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.packet.Packet
import com.envarcade.brennon.messaging.packet.PunishmentPacket
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Core implementation of the PunishmentManager.
 *
 * Handles issuing, revoking, and querying punishments with
 * cross-server sync via Redis and persistent storage.
 */
class CorePunishmentManager(
    private val database: DatabaseManager,
    private val playerManager: CorePlayerManager,
    private val messaging: RedisMessagingService,
    private val eventBus: CoreEventBus,
    private val networkId: String? = null
) : PunishmentManager {

    /** Optional stats tracking — set by Brennon bootstrap when stats module is enabled. */
    var statsTracker: ((UUID, String) -> Unit)? = null

    override fun ban(uuid: UUID, reason: String, duration: Duration?, issuer: UUID?): CompletableFuture<Punishment> {
        return issuePunishment(uuid, PunishmentType.BAN, reason, duration, issuer).thenApply { punishment ->
            // Kick the player if they're online
            kickIfOnline(uuid, formatBanMessage(punishment))
            punishment
        }
    }

    override fun mute(uuid: UUID, reason: String, duration: Duration?, issuer: UUID?): CompletableFuture<Punishment> {
        return issuePunishment(uuid, PunishmentType.MUTE, reason, duration, issuer)
    }

    override fun kick(uuid: UUID, reason: String, issuer: UUID?): CompletableFuture<Punishment> {
        return issuePunishment(uuid, PunishmentType.KICK, reason, null, issuer).thenApply { punishment ->
            kickIfOnline(uuid, formatKickMessage(punishment))
            punishment
        }
    }

    override fun warn(uuid: UUID, reason: String, issuer: UUID?): CompletableFuture<Punishment> {
        return issuePunishment(uuid, PunishmentType.WARN, reason, null, issuer)
    }

    override fun unban(uuid: UUID, issuer: UUID?): CompletableFuture<Void> {
        return revokePunishments(uuid, PunishmentType.BAN, issuer)
    }

    override fun unmute(uuid: UUID, issuer: UUID?): CompletableFuture<Void> {
        return revokePunishments(uuid, PunishmentType.MUTE, issuer)
    }

    override fun getActivePunishments(uuid: UUID): CompletableFuture<List<Punishment>> {
        return database.punishments.findActiveByTarget(uuid).thenApply { list ->
            list.filter { it.isEffectivelyActive() }
                .map { CorePunishment(it) }
        }
    }

    override fun getHistory(uuid: UUID): CompletableFuture<List<Punishment>> {
        return database.punishments.findByTarget(uuid).thenApply { list ->
            list.map { CorePunishment(it) }
        }
    }

    override fun isBanned(uuid: UUID): CompletableFuture<Boolean> {
        return database.punishments.findActiveByTargetAndType(uuid, PunishmentType.BAN).thenApply { list ->
            list.any { it.isEffectivelyActive() }
        }
    }

    override fun isMuted(uuid: UUID): CompletableFuture<Boolean> {
        return database.punishments.findActiveByTargetAndType(uuid, PunishmentType.MUTE).thenApply { list ->
            list.any { it.isEffectivelyActive() }
        }
    }

    override fun ipBan(target: UUID, ip: String, reason: String, duration: Duration?, issuer: UUID?): CompletableFuture<Punishment> {
        return issuePunishment(target, PunishmentType.IP_BAN, reason, duration, issuer, targetIp = ip).thenApply { punishment ->
            kickIfOnline(target, formatBanMessage(punishment))
            punishment
        }
    }

    override fun isIpBanned(ip: String): CompletableFuture<Boolean> {
        return database.punishments.findActiveByIp(ip).thenApply { list ->
            list.any { it.isEffectivelyActive() }
        }
    }

    override fun unIpBan(ip: String, issuer: UUID?): CompletableFuture<Void> {
        return database.punishments.findActiveByIp(ip).thenCompose { list ->
            val futures = list.filter { it.isEffectivelyActive() }.map { data ->
                data.active = false
                data.revokedBy = issuer
                data.revokedAt = Instant.now()
                database.punishments.save(data).thenRun {
                    eventBus.publish(PunishmentRevokedEvent(data.id, data.target, issuer, PunishmentType.IP_BAN))
                }
            }
            CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
                messaging.publish(Channels.PUNISHMENT_REVOKED, """
                    {"target":"$ip","type":"IP_BAN","revokedBy":"${issuer ?: "CONSOLE"}"}
                """.trimIndent())
                println("[Brennon] IP_BAN revoked for $ip")
            }
        }
    }

    // ============================================================
    // Internal Methods
    // ============================================================

    /**
     * Issues a punishment and broadcasts it to the network.
     */
    private fun issuePunishment(
        uuid: UUID,
        type: PunishmentType,
        reason: String,
        duration: Duration?,
        issuer: UUID?,
        targetIp: String? = null
    ): CompletableFuture<Punishment> {
        val id = UUIDUtil.generateId()
        val expiresAt = duration?.let { Instant.now().plus(it) }

        val data = PunishmentData(
            id = id,
            target = uuid,
            issuer = issuer,
            type = type,
            reason = reason,
            issuedAt = Instant.now(),
            expiresAt = expiresAt,
            active = true,
            networkId = networkId,
            targetIp = targetIp
        )

        return database.punishments.save(data).thenApply {
            val punishment = CorePunishment(data)

            // Fire event
            eventBus.publish(PunishmentIssuedEvent(id, uuid, issuer, type, reason))

            // Broadcast to network
            val packet = PunishmentPacket(
                punishmentId = id,
                targetUuid = uuid.toString(),
                type = type.name,
                reason = reason,
                duration = duration?.toMillis()
            )
            messaging.publish(Channels.PUNISHMENT_ISSUED, Packet.serialize(packet))

            println("[Brennon] ${type.name} issued to $uuid: $reason (ID: $id)")

            // Staff notification
            messaging.publish(Channels.STAFF_ALERT, """
                {"type":"punishment","action":"issued","punishmentType":"${type.name}","target":"$uuid","reason":"$reason","issuer":"${issuer ?: "CONSOLE"}","duration":"${duration?.toMillis()}"}
            """.trimIndent())

            // Track stats
            val statId = when (type) {
                PunishmentType.BAN -> com.envarcade.brennon.api.stats.StatTypes.TIMES_BANNED
                PunishmentType.MUTE -> com.envarcade.brennon.api.stats.StatTypes.TIMES_MUTED
                PunishmentType.KICK -> com.envarcade.brennon.api.stats.StatTypes.TIMES_KICKED
                PunishmentType.WARN -> com.envarcade.brennon.api.stats.StatTypes.TIMES_WARNED
                PunishmentType.IP_BAN -> com.envarcade.brennon.api.stats.StatTypes.TIMES_IP_BANNED
            }
            statsTracker?.invoke(uuid, statId)

            punishment
        }
    }

    /**
     * Revokes all active punishments of a specific type for a player.
     */
    private fun revokePunishments(uuid: UUID, type: PunishmentType, revokedBy: UUID?): CompletableFuture<Void> {
        return database.punishments.findActiveByTargetAndType(uuid, type).thenCompose { list ->
            val futures = list.filter { it.isEffectivelyActive() }.map { data ->
                data.active = false
                data.revokedBy = revokedBy
                data.revokedAt = Instant.now()
                database.punishments.save(data).thenRun {
                    eventBus.publish(PunishmentRevokedEvent(data.id, uuid, revokedBy, type))
                }
            }
            CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
                messaging.publish(Channels.PUNISHMENT_REVOKED, """
                    {"target":"$uuid","type":"${type.name}","revokedBy":"${revokedBy ?: "CONSOLE"}"}
                """.trimIndent())

                // Staff notification
                messaging.publish(Channels.STAFF_ALERT, """
                    {"type":"punishment","action":"revoked","punishmentType":"${type.name}","target":"$uuid","revokedBy":"${revokedBy ?: "CONSOLE"}"}
                """.trimIndent())

                println("[Brennon] ${type.name} revoked for $uuid")
            }
        }
    }

    /**
     * Kicks a player from the network if they're online.
     */
    private fun kickIfOnline(uuid: UUID, message: String) {
        messaging.publish(Channels.PLAYER_KICK, """
            {"uuid":"$uuid","reason":"$message"}
        """.trimIndent())
    }

    private fun formatBanMessage(punishment: Punishment): String {
        return buildString {
            appendLine("§c§lYou have been banned from the network.")
            appendLine()
            appendLine("§7Reason: §f${punishment.reason}")
            if (punishment.isPermanent) {
                appendLine("§7Duration: §cPermanent")
            } else {
                appendLine("§7Expires: §f${punishment.expiresAt}")
            }
            appendLine()
            appendLine("§7Punishment ID: §f${punishment.id}")
        }
    }

    private fun formatKickMessage(punishment: Punishment): String {
        return buildString {
            appendLine("§c§lYou have been kicked from the network.")
            appendLine()
            appendLine("§7Reason: §f${punishment.reason}")
        }
    }
}
