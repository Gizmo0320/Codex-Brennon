package com.envarcade.brennon.api.ticket;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a support ticket created by a player.
 */
public interface Ticket {

    /** Unique ticket ID (e.g., "T-1042"). */
    String getId();

    /** The player who created the ticket. */
    UUID getCreator();

    /** The creator's name at time of creation. */
    String getCreatorName();

    /** The staff member assigned to this ticket, or null. */
    UUID getAssignee();

    /** Ticket subject/title. */
    String getSubject();

    /** Current status. */
    TicketStatus getStatus();

    /** Priority level. */
    TicketPriority getPriority();

    /** The server the ticket was created on. */
    String getServer();

    /** When the ticket was created. */
    Instant getCreatedAt();

    /** When the ticket was last updated. */
    Instant getUpdatedAt();

    /** When the ticket was closed, or null. */
    Instant getClosedAt();

    /** All messages/replies in the ticket thread. */
    List<TicketMessage> getMessages();
}
