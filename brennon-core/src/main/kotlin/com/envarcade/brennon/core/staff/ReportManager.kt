package com.envarcade.brennon.core.staff

import com.envarcade.brennon.common.util.UUIDUtil
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import com.google.gson.Gson
import java.time.Instant
import java.util.UUID

/**
 * Manages player reports filed by players or staff.
 *
 * Reports are stored in Redis for real-time cross-server access
 * and broadcast to online staff members. When network-scoped,
 * report keys include the networkId for isolation.
 */
class ReportManager(
    private val messaging: RedisMessagingService,
    private val serverName: String,
    private val reportNetworkId: String? = null
) {

    private val gson = Gson()

    /** Redis key prefix — network-scoped if configured. */
    private val reportKeyPrefix: String =
        if (reportNetworkId != null) "brennon:report:$reportNetworkId:" else "brennon:report:"

    /** Optional stats tracking — set by Brennon bootstrap when stats module is enabled. */
    var statsTracker: ((UUID, String) -> Unit)? = null

    data class Report(
        val id: String = UUIDUtil.generateId(),
        val reporter: UUID,
        val reporterName: String,
        val target: UUID,
        val targetName: String,
        val reason: String,
        val server: String,
        val timestamp: Long = Instant.now().toEpochMilli(),
        var status: ReportStatus = ReportStatus.OPEN,
        var handledBy: UUID? = null
    )

    enum class ReportStatus {
        OPEN, IN_PROGRESS, RESOLVED, DISMISSED
    }

    /**
     * Creates a new report and broadcasts it to staff.
     */
    fun createReport(reporter: UUID, reporterName: String, target: UUID, targetName: String, reason: String): Report {
        val report = Report(
            reporter = reporter,
            reporterName = reporterName,
            target = target,
            targetName = targetName,
            reason = reason,
            server = serverName
        )

        // Store in Redis
        try {
            messaging.getPool().resource.use { jedis ->
                jedis.hset("${reportKeyPrefix}${report.id}", mapOf(
                    "reporter" to reporter.toString(),
                    "reporterName" to reporterName,
                    "target" to target.toString(),
                    "targetName" to targetName,
                    "reason" to reason,
                    "server" to serverName,
                    "timestamp" to report.timestamp.toString(),
                    "status" to report.status.name
                ))
                // Reports expire after 24 hours
                jedis.expire("${reportKeyPrefix}${report.id}", 86400)
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to store report: ${e.message}")
        }

        // Track stats
        statsTracker?.invoke(reporter, com.envarcade.brennon.api.stats.StatTypes.REPORTS_FILED)
        statsTracker?.invoke(target, com.envarcade.brennon.api.stats.StatTypes.REPORTS_RECEIVED)

        // Broadcast to staff
        messaging.publish(Channels.STAFF_ALERT, """
            {"type":"report","id":"${report.id}","reporter":"$reporterName","target":"$targetName","reason":"$reason","server":"$serverName"}
        """.trimIndent())

        return report
    }

    /**
     * Gets all open reports from Redis.
     */
    fun getOpenReports(): List<Report> {
        return try {
            messaging.getPool().resource.use { jedis ->
                val keys = jedis.keys("${reportKeyPrefix}*")
                keys.mapNotNull { key ->
                    val data = jedis.hgetAll(key)
                    if (data["status"] == ReportStatus.OPEN.name || data["status"] == ReportStatus.IN_PROGRESS.name) {
                        Report(
                            id = key.removePrefix(reportKeyPrefix),
                            reporter = UUID.fromString(data["reporter"]),
                            reporterName = data["reporterName"] ?: "Unknown",
                            target = UUID.fromString(data["target"]),
                            targetName = data["targetName"] ?: "Unknown",
                            reason = data["reason"] ?: "",
                            server = data["server"] ?: "",
                            timestamp = data["timestamp"]?.toLongOrNull() ?: 0,
                            status = ReportStatus.valueOf(data["status"] ?: "OPEN")
                        )
                    } else null
                }.sortedByDescending { it.timestamp }
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to fetch reports: ${e.message}")
            emptyList()
        }
    }

    /**
     * Claims a report (sets status to IN_PROGRESS).
     */
    fun claimReport(reportId: String, staffUuid: UUID): Boolean {
        return try {
            messaging.getPool().resource.use { jedis ->
                jedis.hset("${reportKeyPrefix}$reportId", mapOf(
                    "status" to ReportStatus.IN_PROGRESS.name,
                    "handledBy" to staffUuid.toString()
                ))
                true
            }
        } catch (_: Exception) { false }
    }

    /**
     * Resolves a report.
     */
    fun resolveReport(reportId: String, status: ReportStatus): Boolean {
        return try {
            messaging.getPool().resource.use { jedis ->
                jedis.hset("${reportKeyPrefix}$reportId", "status", status.name)
                true
            }
        } catch (_: Exception) { false }
    }
}
