package com.envarcade.brennon.webserver.data

data class AppealData(
    val id: String,
    val punishmentId: String,
    val playerUuid: String,
    val playerName: String,
    val reason: String,
    val status: AppealStatus,
    val staffResponse: String?,
    val staffUuid: String?,
    val createdAt: Long,
    val resolvedAt: Long?
)

enum class AppealStatus {
    PENDING, APPROVED, DENIED
}
