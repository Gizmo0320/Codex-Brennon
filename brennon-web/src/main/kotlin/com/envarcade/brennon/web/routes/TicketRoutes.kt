package com.envarcade.brennon.web.routes

import com.envarcade.brennon.core.Brennon
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.envarcade.brennon.api.ticket.TicketStatus
import spark.Spark
import java.util.UUID

class TicketRoutes(private val brennon: Brennon) {

    private val gson = Gson()

    fun register() {
        Spark.get("/api/tickets") { _, _ ->
            val tickets = brennon.coreTicketManager.getOpenTickets().join()
            gson.toJson(tickets.map { t ->
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

        Spark.get("/api/tickets/:id") { req, _ ->
            val id = req.params("id").uppercase()
            val ticket = brennon.coreTicketManager.getTicket(id).join()
            if (ticket.isEmpty) {
                Spark.halt(404, """{"error":"Ticket not found"}""")
            }
            val t = ticket.get()
            gson.toJson(mapOf(
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

        Spark.get("/api/tickets/player/:uuid") { req, _ ->
            val uuid = UUID.fromString(req.params("uuid"))
            val tickets = brennon.coreTicketManager.getPlayerTickets(uuid).join()
            gson.toJson(tickets.map { t ->
                mapOf(
                    "id" to t.id,
                    "subject" to t.subject,
                    "status" to t.status,
                    "priority" to t.priority
                )
            })
        }

        Spark.post("/api/tickets") { req, _ ->
            val body = JsonParser.parseString(req.body()).asJsonObject
            val creator = UUID.fromString(body.get("creator").asString)
            val creatorName = body.get("creatorName").asString
            val subject = body.get("subject").asString
            val description = if (body.has("description")) body.get("description").asString else subject
            val server = if (body.has("server")) body.get("server").asString else "web"

            val ticket = brennon.coreTicketManager.createTicket(creator, creatorName, subject, description, server).join()
            gson.toJson(mapOf("success" to true, "id" to ticket.id))
        }

        Spark.post("/api/tickets/:id/reply") { req, _ ->
            val id = req.params("id").uppercase()
            val body = JsonParser.parseString(req.body()).asJsonObject
            val author = UUID.fromString(body.get("author").asString)
            val authorName = body.get("authorName").asString
            val content = body.get("content").asString
            val isStaff = if (body.has("isStaff")) body.get("isStaff").asBoolean else false

            brennon.coreTicketManager.addReply(id, author, authorName, content, isStaff).join()
            gson.toJson(mapOf("success" to true))
        }

        Spark.put("/api/tickets/:id/assign") { req, _ ->
            val id = req.params("id").uppercase()
            val body = JsonParser.parseString(req.body()).asJsonObject
            val assignee = UUID.fromString(body.get("assignee").asString)

            brennon.coreTicketManager.assignTicket(id, assignee).join()
            gson.toJson(mapOf("success" to true))
        }

        Spark.put("/api/tickets/:id/status") { req, _ ->
            val id = req.params("id").uppercase()
            val body = JsonParser.parseString(req.body()).asJsonObject
            val status = TicketStatus.valueOf(body.get("status").asString.uppercase())

            brennon.coreTicketManager.setStatus(id, status).join()
            gson.toJson(mapOf("success" to true))
        }

        Spark.put("/api/tickets/:id/close") { req, _ ->
            val id = req.params("id").uppercase()
            val body = JsonParser.parseString(req.body()).asJsonObject
            val closedBy = UUID.fromString(body.get("closedBy").asString)

            brennon.coreTicketManager.closeTicket(id, closedBy).join()
            gson.toJson(mapOf("success" to true))
        }
    }
}
