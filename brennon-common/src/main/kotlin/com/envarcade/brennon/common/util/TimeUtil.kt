package com.envarcade.brennon.common.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtil {

    private val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun format(instant: Instant): String = formatter.format(instant)

    fun formatDuration(duration: Duration?): String {
        if (duration == null) return "permanent"
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 && days == 0L) append("${seconds}s")
        }.trim().ifEmpty { "0s" }
    }

    fun parseDuration(input: String): Duration? {
        if (input.equals("permanent", ignoreCase = true) || input.equals("perm", ignoreCase = true)) {
            return null
        }
        val regex = Regex("(\\d+)([dhms])")
        val matches = regex.findAll(input.lowercase())
        if (!matches.any()) return null
        var duration = Duration.ZERO
        for (match in matches) {
            val amount = match.groupValues[1].toLong()
            when (match.groupValues[2]) {
                "d" -> duration = duration.plusDays(amount)
                "h" -> duration = duration.plusHours(amount)
                "m" -> duration = duration.plusMinutes(amount)
                "s" -> duration = duration.plusSeconds(amount)
            }
        }
        return duration
    }

    fun getRemaining(expiration: Instant?): String {
        if (expiration == null) return "permanent"
        val remaining = Duration.between(Instant.now(), expiration)
        return if (remaining.isNegative) "expired" else formatDuration(remaining)
    }
}
