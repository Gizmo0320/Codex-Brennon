package com.envarcade.brennon.common.model

import java.time.Instant
import java.util.UUID

data class TicketMessageData(
    val author: UUID,
    val authorName: String,
    val content: String,
    val timestamp: Instant = Instant.now(),
    val isStaffMessage: Boolean = false
)
