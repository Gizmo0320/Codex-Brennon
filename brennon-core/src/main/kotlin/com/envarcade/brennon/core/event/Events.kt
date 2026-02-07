package com.envarcade.brennon.core.event

import com.envarcade.brennon.api.event.BrennonEvent
import com.envarcade.brennon.api.punishment.PunishmentType
import com.envarcade.brennon.api.ticket.TicketStatus
import java.util.UUID

// ============================================================
// Player Events
// ============================================================

/**
 * Fired when a player joins the network (first seen by any server).
 */
class PlayerNetworkJoinEvent(
    val uuid: UUID,
    val name: String,
    val server: String
) : BrennonEvent()

/**
 * Fired when a player leaves the network entirely.
 */
class PlayerNetworkQuitEvent(
    val uuid: UUID,
    val name: String,
    val lastServer: String
) : BrennonEvent()

/**
 * Fired when a player switches servers within the network.
 */
class PlayerServerSwitchEvent(
    val uuid: UUID,
    val name: String,
    val fromServer: String,
    val toServer: String
) : BrennonEvent()

// ============================================================
// Rank Events
// ============================================================

/**
 * Fired when a player's rank is changed.
 */
class PlayerRankChangeEvent(
    val uuid: UUID,
    val oldRank: String,
    val newRank: String,
    val changedBy: UUID?
) : BrennonEvent()

// ============================================================
// Economy Events
// ============================================================

/**
 * Fired when a player's balance changes.
 */
class BalanceChangeEvent(
    val uuid: UUID,
    val oldBalance: Double,
    val newBalance: Double,
    val reason: String
) : BrennonEvent()

/**
 * Fired when money is transferred between players.
 */
class TransferEvent(
    val from: UUID,
    val to: UUID,
    val amount: Double
) : BrennonEvent()

// ============================================================
// Punishment Events
// ============================================================

/**
 * Fired when a punishment is issued.
 */
class PunishmentIssuedEvent(
    val punishmentId: String,
    val target: UUID,
    val issuer: UUID?,
    val type: PunishmentType,
    val reason: String
) : BrennonEvent()

/**
 * Fired when a punishment is revoked.
 */
class PunishmentRevokedEvent(
    val punishmentId: String,
    val target: UUID,
    val revokedBy: UUID?,
    val type: PunishmentType
) : BrennonEvent()

// ============================================================
// Chat Events
// ============================================================

class ChatMessageEvent(
    val sender: UUID,
    val senderName: String,
    val channelId: String,
    val message: String,
    val server: String
) : BrennonEvent()

class PrivateMessageEvent(
    val sender: UUID,
    val senderName: String,
    val recipient: UUID,
    val message: String
) : BrennonEvent()

class ChannelSwitchEvent(
    val player: UUID,
    val oldChannel: String,
    val newChannel: String
) : BrennonEvent()

// ============================================================
// Ticket Events
// ============================================================

class TicketCreateEvent(
    val ticketId: String,
    val creator: UUID,
    val creatorName: String,
    val subject: String
) : BrennonEvent()

class TicketReplyEvent(
    val ticketId: String,
    val author: UUID,
    val authorName: String,
    val isStaff: Boolean
) : BrennonEvent()

class TicketStatusChangeEvent(
    val ticketId: String,
    val oldStatus: TicketStatus,
    val newStatus: TicketStatus,
    val changedBy: UUID?
) : BrennonEvent()

class TicketAssignEvent(
    val ticketId: String,
    val assignee: UUID,
    val assignedBy: UUID?
) : BrennonEvent()

// ============================================================
// Stats Events
// ============================================================

class StatChangeEvent(
    val player: UUID,
    val statId: String,
    val oldValue: Double,
    val newValue: Double
) : BrennonEvent()

// ============================================================
// Server Registry Events
// ============================================================

class ServerRegisteredEvent(
    val serverName: String,
    val group: String,
    val host: String,
    val port: Int,
    val autoRegistered: Boolean
) : BrennonEvent()

class ServerUnregisteredEvent(
    val serverName: String,
    val group: String
) : BrennonEvent()

class ServerGroupCreatedEvent(
    val groupId: String,
    val displayName: String
) : BrennonEvent()

class ServerGroupDeletedEvent(
    val groupId: String
) : BrennonEvent()

// ============================================================
// Rank Sync Events
// ============================================================

/**
 * Fired when a rank definition is synced between Brennon and LuckPerms.
 */
class RankSyncEvent(
    val rankId: String,
    val source: String
) : BrennonEvent()

/**
 * Fired when a rank is deleted and the deletion is synced.
 */
class RankDeleteSyncEvent(
    val rankId: String,
    val source: String
) : BrennonEvent()
