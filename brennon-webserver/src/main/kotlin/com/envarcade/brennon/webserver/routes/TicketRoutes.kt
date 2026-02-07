package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.api.ticket.TicketPriority
import com.envarcade.brennon.api.ticket.TicketStatus
import com.envarcade.brennon.core.Brennon
import io.javalin.Javalin
import java.util.UUID

class TicketRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/tickets") { ctx ->
            val tickets = brennon.coreTicketManager.getOpenTickets().join()
            ctx.json(tickets.map { t ->
                mapOf(
                    "id" to t.id,
                    "subject" to t.subject,
                    "status" to t.status,
                    "priority" to t.priority,
                    "creatorName" to t.creatorName,
                    "server" to t.server,
                    "assignee" to t.assignee?.toString()
                )
            })
        }

        app.get("/api/tickets/{id}") { ctx ->
            val id = ctx.pathParam("id").uppercase()
            val ticket = brennon.coreTicketManager.getTicket(id).join()
            if (ticket.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Ticket not found"))
                return@get
            }
            val t = ticket.get()
            ctx.json(mapOf(
                "id" to t.id,
                "subject" to t.subject,
                "status" to t.status,
                "priority" to t.priority,
                "creatorName" to t.creatorName,
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

        app.get("/api/tickets/player/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val tickets = brennon.coreTicketManager.getPlayerTickets(uuid).join()
            ctx.json(tickets.map { t ->
                mapOf(
                    "id" to t.id,
                    "subject" to t.subject,
                    "status" to t.status,
                    "priority" to t.priority
                )
            })
        }

        app.get("/api/tickets/assigned/{uuid}") { ctx ->
            val uuid = UUID.fromString(ctx.pathParam("uuid"))
            val tickets = brennon.coreTicketManager.getAssignedTickets(uuid).join()
            ctx.json(tickets.map { t ->
                mapOf(
                    "id" to t.id,
                    "subject" to t.subject,
                    "status" to t.status,
                    "priority" to t.priority,
                    "creatorName" to t.creatorName
                )
            })
        }

        app.post("/api/tickets") { ctx ->
            val body = ctx.bodyAsClass(CreateTicketRequest::class.java)
            val creator = UUID.fromString(body.creator)
            val ticket = brennon.coreTicketManager.createTicket(
                creator, body.creatorName, body.subject,
                body.description.ifBlank { body.subject },
                body.server.ifBlank { "web" }
            ).join()
            ctx.json(mapOf("success" to true, "id" to ticket.id))
        }

        app.post("/api/tickets/{id}/reply") { ctx ->
            val id = ctx.pathParam("id").uppercase()
            val body = ctx.bodyAsClass(ReplyRequest::class.java)
            brennon.coreTicketManager.addReply(
                id, UUID.fromString(body.author), body.authorName, body.content, body.isStaff
            ).join()
            ctx.json(mapOf("success" to true))
        }

        app.put("/api/tickets/{id}/assign") { ctx ->
            val id = ctx.pathParam("id").uppercase()
            val body = ctx.bodyAsClass(AssignRequest::class.java)
            brennon.coreTicketManager.assignTicket(id, UUID.fromString(body.assignee)).join()
            ctx.json(mapOf("success" to true))
        }

        app.put("/api/tickets/{id}/status") { ctx ->
            val id = ctx.pathParam("id").uppercase()
            val body = ctx.bodyAsClass(StatusRequest::class.java)
            val status = TicketStatus.valueOf(body.status.uppercase())
            brennon.coreTicketManager.setStatus(id, status).join()
            ctx.json(mapOf("success" to true))
        }

        app.put("/api/tickets/{id}/priority") { ctx ->
            val id = ctx.pathParam("id").uppercase()
            val body = ctx.bodyAsClass(PriorityRequest::class.java)
            val priority = TicketPriority.valueOf(body.priority.uppercase())
            brennon.coreTicketManager.setPriority(id, priority).join()
            ctx.json(mapOf("success" to true))
        }

        app.put("/api/tickets/{id}/close") { ctx ->
            val id = ctx.pathParam("id").uppercase()
            val body = ctx.bodyAsClass(CloseRequest::class.java)
            brennon.coreTicketManager.closeTicket(id, UUID.fromString(body.closedBy)).join()
            ctx.json(mapOf("success" to true))
        }
    }

    data class CreateTicketRequest(
        val creator: String = "", val creatorName: String = "",
        val subject: String = "", val description: String = "", val server: String = ""
    )
    data class ReplyRequest(
        val author: String = "", val authorName: String = "",
        val content: String = "", val isStaff: Boolean = false
    )
    data class AssignRequest(val assignee: String = "")
    data class StatusRequest(val status: String = "")
    data class PriorityRequest(val priority: String = "")
    data class CloseRequest(val closedBy: String = "")
}
