package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.staff.ReportManager
import io.javalin.Javalin
import java.util.UUID

class ReportRoutes(private val brennon: Brennon) {

    fun register(app: Javalin) {
        app.get("/api/reports") { ctx ->
            val reports = brennon.reportManager.getOpenReports()
            ctx.json(reports.map { r ->
                mapOf(
                    "id" to r.id,
                    "reporter" to r.reporter.toString(),
                    "reporterName" to r.reporterName,
                    "target" to r.target.toString(),
                    "targetName" to r.targetName,
                    "reason" to r.reason,
                    "server" to r.server,
                    "timestamp" to r.timestamp,
                    "status" to r.status.name,
                    "handledBy" to r.handledBy?.toString()
                )
            })
        }

        app.post("/api/reports") { ctx ->
            val body = ctx.bodyAsClass(CreateReportRequest::class.java)
            val report = brennon.reportManager.createReport(
                UUID.fromString(body.reporter), body.reporterName,
                UUID.fromString(body.target), body.targetName,
                body.reason
            )
            ctx.json(mapOf("success" to true, "id" to report.id))
        }

        app.put("/api/reports/{id}/claim") { ctx ->
            val id = ctx.pathParam("id")
            val body = ctx.bodyAsClass(ClaimRequest::class.java)
            val claimed = brennon.reportManager.claimReport(id, UUID.fromString(body.staffUuid))
            if (!claimed) {
                ctx.status(400).json(mapOf("error" to "Report not found or already claimed"))
                return@put
            }
            ctx.json(mapOf("success" to true))
        }

        app.put("/api/reports/{id}/resolve") { ctx ->
            val id = ctx.pathParam("id")
            val body = ctx.bodyAsClass(ResolveRequest::class.java)
            val status = ReportManager.ReportStatus.valueOf(body.status.uppercase())
            val resolved = brennon.reportManager.resolveReport(id, status)
            if (!resolved) {
                ctx.status(400).json(mapOf("error" to "Report not found"))
                return@put
            }
            ctx.json(mapOf("success" to true))
        }
    }

    data class CreateReportRequest(
        val reporter: String = "", val reporterName: String = "",
        val target: String = "", val targetName: String = "",
        val reason: String = ""
    )
    data class ClaimRequest(val staffUuid: String = "")
    data class ResolveRequest(val status: String = "")
}
