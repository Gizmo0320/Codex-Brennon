package com.envarcade.brennon.api.ticket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages support tickets across the network.
 */
public interface TicketManager {

    /** Creates a new ticket. */
    CompletableFuture<Ticket> createTicket(UUID creator, String creatorName, String subject, String message, String server);

    /** Gets a ticket by ID. */
    CompletableFuture<Optional<Ticket>> getTicket(String id);

    /** Gets all open tickets. */
    CompletableFuture<List<Ticket>> getOpenTickets();

    /** Gets all tickets created by a player. */
    CompletableFuture<List<Ticket>> getPlayerTickets(UUID player);

    /** Gets tickets assigned to a staff member. */
    CompletableFuture<List<Ticket>> getAssignedTickets(UUID staff);

    /** Adds a reply to a ticket. */
    CompletableFuture<Void> addReply(String ticketId, UUID author, String authorName, String message, boolean isStaff);

    /** Assigns a ticket to a staff member. */
    CompletableFuture<Void> assignTicket(String ticketId, UUID staff);

    /** Changes the status of a ticket. */
    CompletableFuture<Void> setStatus(String ticketId, TicketStatus status);

    /** Changes the priority of a ticket. */
    CompletableFuture<Void> setPriority(String ticketId, TicketPriority priority);

    /** Closes a ticket. */
    CompletableFuture<Void> closeTicket(String ticketId, UUID closedBy);
}
