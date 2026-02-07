package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.api.stats.StatTypes
import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.common.util.TimeUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender
import java.time.Duration

/**
 * /stats [player] [--network <id>] — View player statistics
 */
class StatsCommand(private val brennon: Brennon) : BrennonCommand(
    name = "stats",
    permission = "brennon.command.stats",
    usage = "/stats [player] [--network <id>]",
    description = "View player statistics"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        // Parse --network flag
        var targetNetworkId: String? = null
        val filteredArgs = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            if (args[i].equals("--network", ignoreCase = true) && i + 1 < args.size) {
                targetNetworkId = args[i + 1]
                i += 2
            } else {
                filteredArgs.add(args[i])
                i++
            }
        }

        if (targetNetworkId != null && !sender.hasPermission("brennon.admin.crossnetwork")) {
            sender.sendMessage(TextUtil.error("You don't have permission to view cross-network stats."))
            return
        }

        if (filteredArgs.isEmpty()) {
            if (!sender.isPlayer) {
                sender.sendMessage(TextUtil.error("Usage: /stats <player>"))
                return
            }
            showStats(sender, sender.uuid!!, sender.name, targetNetworkId)
        } else {
            val targetName = filteredArgs[0]
            brennon.corePlayerManager.getPlayer(targetName).thenAccept { optPlayer ->
                if (optPlayer.isEmpty) {
                    sender.sendMessage(TextUtil.error("Player not found: <white>$targetName"))
                    return@thenAccept
                }
                showStats(sender, optPlayer.get().uniqueId, optPlayer.get().name, targetNetworkId)
            }
        }
    }

    private fun showStats(sender: BrennonCommandSender, uuid: java.util.UUID, name: String, networkId: String?) {
        val statsFuture = if (networkId != null) {
            brennon.crossNetworkService.getStatsForNetwork(uuid, networkId)
        } else {
            brennon.coreStatsManager.getAllStats(uuid)
        }

        val label = if (networkId != null) " <aqua>[$networkId]</aqua>" else ""
        statsFuture.thenAccept { stats ->
            sender.sendMessage(TextUtil.prefixed("Statistics for <yellow>$name</yellow>$label:"))
            if (stats.isEmpty()) {
                sender.sendMessage(TextUtil.parse("  <gray>No stats recorded yet."))
                return@thenAccept
            }
            for ((statId, value) in stats) {
                if (value == 0.0) continue
                val displayName = statId.replace("_", " ").replaceFirstChar { it.uppercase() }
                val displayValue = if (statId == StatTypes.PLAYTIME) {
                    TimeUtil.formatDuration(Duration.ofMillis(value.toLong() * 1000))
                } else if (value == value.toLong().toDouble()) {
                    value.toLong().toString()
                } else {
                    String.format("%.2f", value)
                }
                sender.sendMessage(TextUtil.parse("  <gray>$displayName: <yellow>$displayValue"))
            }
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        return when {
            args.size == 1 -> brennon.corePlayerManager.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(args[0], ignoreCase = true) }
            args.last().equals("--network", ignoreCase = true) || args.dropLast(1).lastOrNull()?.equals("--network", ignoreCase = true) == true -> emptyList()
            sender.hasPermission("brennon.admin.crossnetwork") -> listOf("--network").filter { it.startsWith(args.last(), ignoreCase = true) }
            else -> emptyList()
        }
    }
}

/**
 * /leaderboard <stat> [page] — View stat leaderboards
 */
class LeaderboardCommand(private val brennon: Brennon) : BrennonCommand(
    name = "leaderboard",
    permission = "brennon.command.leaderboard",
    aliases = listOf("lb", "top"),
    usage = "/leaderboard <stat> [page]",
    description = "View stat leaderboards"
) {
    private val statNames = listOf(
        StatTypes.PLAYTIME, StatTypes.SESSIONS, StatTypes.KILLS, StatTypes.DEATHS,
        StatTypes.KDR, StatTypes.KILL_STREAK, StatTypes.HIGHEST_KILL_STREAK,
        StatTypes.MONEY_EARNED, StatTypes.MONEY_SPENT, StatTypes.BLOCKS_PLACED,
        StatTypes.BLOCKS_BROKEN, StatTypes.MESSAGES_SENT, StatTypes.TIMES_BANNED,
        StatTypes.TIMES_MUTED, StatTypes.TIMES_KICKED, StatTypes.TIMES_WARNED,
        StatTypes.REPORTS_FILED, StatTypes.REPORTS_RECEIVED,
        StatTypes.TICKETS_CREATED, StatTypes.TICKETS_RESOLVED
    )

    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(TextUtil.error("Usage: /leaderboard <stat> [page]"))
            sender.sendMessage(TextUtil.parse("  <gray>Available stats: <yellow>${statNames.joinToString(", ")}"))
            return
        }

        val statId = args[0].lowercase()
        val page = if (args.size > 1) (args[1].toIntOrNull() ?: 1) else 1
        val perPage = 10

        brennon.coreStatsManager.getLeaderboard(statId, perPage * page).thenAccept { leaderboard ->
            if (leaderboard.isEmpty()) {
                sender.sendMessage(TextUtil.prefixed("No data for stat: <yellow>$statId"))
                return@thenAccept
            }

            val displayName = statId.replace("_", " ").replaceFirstChar { it.uppercase() }
            sender.sendMessage(TextUtil.prefixed("Leaderboard: <yellow>$displayName <gray>(Page $page)"))

            val entries = leaderboard.entries.toList()
            val startIndex = (page - 1) * perPage
            val endIndex = minOf(startIndex + perPage, entries.size)

            if (startIndex >= entries.size) {
                sender.sendMessage(TextUtil.parse("  <gray>No more entries."))
                return@thenAccept
            }

            for (i in startIndex until endIndex) {
                val (uuid, value) = entries[i]
                val playerName = brennon.corePlayerManager.getCachedPlayer(uuid)?.name ?: uuid.toString().substring(0, 8)
                val displayValue = if (statId == StatTypes.PLAYTIME) {
                    TimeUtil.formatDuration(Duration.ofMillis(value.toLong() * 1000))
                } else if (value == value.toLong().toDouble()) {
                    value.toLong().toString()
                } else {
                    String.format("%.2f", value)
                }
                val rank = i + 1
                val color = when (rank) {
                    1 -> "<gold>"
                    2 -> "<gray>"
                    3 -> "<#CD7F32>"
                    else -> "<white>"
                }
                sender.sendMessage(TextUtil.parse("  $color#$rank <yellow>$playerName <dark_gray>- <white>$displayValue"))
            }
        }
    }

    override fun tabComplete(sender: BrennonCommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return statNames.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}
