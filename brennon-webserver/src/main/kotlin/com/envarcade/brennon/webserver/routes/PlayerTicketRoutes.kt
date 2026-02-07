package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.webserver.WebServerConfig
import com.envarcade.brennon.webserver.auth.JwtAuth
import com.envarcade.brennon.webserver.auth.RoleMiddleware
import io.javalin.Javalin
import java.util.UUID

class PlayerTicketRoutes(
    private val brennon: Brennon,
    private val jwtAuth: JwtAuth,
    private val config: WebServerConfig
) {

    fun register(app: Javalin) {

        // Player: get own tickets
        app.get("/api/player/tickets") { ctx ->
            if (!RoleMiddleware.requirePlayer(ctx, jwtAuth, config.apiKey)) return@get
            val uuid = RoleMiddleware.getAuthPlayerUuid(ctx, jwtAuth)
            if (uuid == null) {
                ctx.status(400).json(mapOf("error" to "Player UUID not found in token"))
                return@get
            }

            val tickets = brennon.coreTicketManager.getPlayerTickets(uuid).join()
            ctx.json(tickets.map { t ->
                mapOf(
                    "id" to t.id,
                    "subject" to t.subject,
                    "status" to t.status,
                    "priority" to t.priority,
                    "server" to t.server
                )
            })
        }

        // Player: get own ticket detail
        app.get("/api/player/tickets/{id}") { ctx ->
            if (!RoleMiddleware.requirePlayer(ctx, jwtAuth, config.apiKey)) return@get
            val uuid = RoleMiddleware.getAuthPlayerUuid(ctx, jwtAuth)
            if (uuid == null) {
                ctx.status(400).json(mapOf("error" to "Player UUID not found in token"))
                return@get
            }

            val id = ctx.pathParam("id").uppercase()
            val ticket = brennon.coreTicketManager.getTicket(id).join()
            if (ticket.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Ticket not found"))
                return@get
            }

            val t = ticket.get()
            // Verify the ticket belongs to this player
            if (t.creator != uuid) {
                ctx.status(403).json(mapOf("error" to "This ticket does not belong to you"))
                return@get
            }

            ctx.json(mapOf(
                "id" to t.id,
                "subject" to t.subject,
                "status" to t.status,
                "priority" to t.priority,
                "server" to t.server,
                "assignee" to t.assignee?.toString(),
                "messages" to t.messages.map { m ->
                    mapOf(
                        "authorName" to m.authorName,
                        "content" to m.content,
                        "timestamp" to m.timestamp.toString(),
                        "isStaffMessage" to m.isStaffMessage
                    )
                }
            ))
        }

        // Player: create ticket
        app.post("/api/player/tickets") { ctx ->
            if (!RoleMiddleware.requirePlayer(ctx, jwtAuth, config.apiKey)) return@post
            val uuid = RoleMiddleware.getAuthPlayerUuid(ctx, jwtAuth)
            val playerName = RoleMiddleware.getAuthUsername(ctx, jwtAuth) ?: "Unknown"
            if (uuid == null) {
                ctx.status(400).json(mapOf("error" to "Player UUID not found in token"))
                return@post
            }

            val body = ctx.bodyAsClass(CreateTicketRequest::class.java)
            if (body.subject.isBlank()) {
                ctx.status(400).json(mapOf("error" to "subject is required"))
                return@post
            }

            val ticket = brennon.coreTicketManager.createTicket(
                uuid, playerName, body.subject,
                body.description.ifBlank { body.subject },
                "web"
            ).join()

            ctx.json(mapOf("success" to true, "id" to ticket.id))
        }

        // Player: reply to own ticket
        app.post("/api/player/tickets/{id}/reply") { ctx ->
            if (!RoleMiddleware.requirePlayer(ctx, jwtAuth, config.apiKey)) return@post
            val uuid = RoleMiddleware.getAuthPlayerUuid(ctx, jwtAuth)
            val playerName = RoleMiddleware.getAuthUsername(ctx, jwtAuth) ?: "Unknown"
            if (uuid == null) {
                ctx.status(400).json(mapOf("error" to "Player UUID not found in token"))
                return@post
            }

            val id = ctx.pathParam("id").uppercase()

            // Verify the ticket belongs to this player
            val ticket = brennon.coreTicketManager.getTicket(id).join()
            if (ticket.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Ticket not found"))
                return@post
            }
            if (ticket.get().creator != uuid) {
                ctx.status(403).json(mapOf("error" to "This ticket does not belong to you"))
                return@post
            }

            val body = ctx.bodyAsClass(ReplyRequest::class.java)
            if (body.content.isBlank()) {
                ctx.status(400).json(mapOf("error" to "content is required"))
                return@post
            }

            brennon.coreTicketManager.addReply(id, uuid, playerName, body.content, false).join()
            ctx.json(mapOf("success" to true))
        }
    }

    data class CreateTicketRequest(val subject: String = "", val description: String = "")
    data class ReplyRequest(val content: String = "")
}
