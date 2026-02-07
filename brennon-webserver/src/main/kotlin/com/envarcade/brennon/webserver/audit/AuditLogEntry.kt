package com.envarcade.brennon.webserver.audit

data class AuditLogEntry(
    val id: String,
    val timestamp: Long,
    val username: String,
    val action: String,
    val path: String,
    val statusCode: Int,
    val targetInfo: String?,
    val requestBody: String?,
    val ipAddress: String?,
    val category: AuditCategory
)

enum class AuditCategory {
    AUTH,
    PLAYER,
    RANK,
    PUNISHMENT,
    ECONOMY,
    TICKET,
    STATS,
    SERVER,
    STAFF,
    REPORT,
    CHAT,
    OTHER
}
