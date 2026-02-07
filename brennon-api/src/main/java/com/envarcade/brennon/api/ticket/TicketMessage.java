package com.envarcade.brennon.api.ticket;

import java.time.Instant;
import java.util.UUID;

/**
 * A single message within a ticket thread.
 */
public interface TicketMessage {

    /** The author of this message. */
    UUID getAuthor();

    /** The author's name. */
    String getAuthorName();

    /** The message content. */
    String getContent();

    /** When this message was sent. */
    Instant getTimestamp();

    /** Whether this message is from staff. */
    boolean isStaffMessage();
}
