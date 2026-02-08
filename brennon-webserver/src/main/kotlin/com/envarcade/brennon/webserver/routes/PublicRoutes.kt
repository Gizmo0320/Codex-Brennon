package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.api.punishment.PunishmentType
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.webserver.data.AppealRepository
import io.javalin.Javalin
import java.util.UUID

class PublicRoutes(
    private val brennon: Brennon,
    private val appealRepository: AppealRepository
) {

    fun register(app: Javalin) {

        // Public: network overview (aggregate stats)
        app.get("/api/public/network") { ctx ->
            val servers = brennon.coreServerManager.getServers().filter { it.isOnline }
            val onlinePlayers = brennon.coreServerManager.getNetworkPlayerCount()
            val totalPlayers = brennon.databaseManager.players.countAll().join()

            ctx.json(mapOf(
                "networkName" to brennon.config.network.displayName,
                "onlinePlayers" to onlinePlayers,
                "totalPlayers" to totalPlayers,
                "serverCount" to servers.size,
                "servers" to servers.map { s ->
                    mapOf(
                        "name" to s.name,
                        "group" to s.group,
                        "playerCount" to s.playerCount,
                        "maxPlayers" to s.maxPlayers
                    )
                }
            ))
        }

        // Public: paginated player list
        app.get("/api/public/players") { ctx ->
            val limit = (ctx.queryParam("limit")?.toIntOrNull() ?: 20).coerceIn(1, 100)
            val offset = (ctx.queryParam("offset")?.toIntOrNull() ?: 0).coerceAtLeast(0)

            val players = brennon.databaseManager.players.findRecent(limit, offset).join()
            val total = brennon.databaseManager.players.countAll().join()

            val entries = players.map { p ->
                mapOf(
                    "uuid" to p.uuid.toString(),
                    "name" to p.name,
                    "primaryRank" to p.primaryRank,
                    "firstJoin" to p.firstJoin.toEpochMilli(),
                    "lastSeen" to p.lastSeen.toEpochMilli(),
                    "online" to (brennon.corePlayerManager.getCachedPlayer(p.uuid)?.isOnline == true)
                )
            }

            ctx.json(mapOf(
                "total" to total,
                "limit" to limit,
                "offset" to offset,
                "players" to entries
            ))
        }

        // Public: rank list
        app.get("/api/public/ranks") { ctx ->
            val ranks = brennon.coreRankManager.getRanks().map { r ->
                mapOf<String, Any>(
                    "id" to r.id,
                    "displayName" to r.displayName,
                    "prefix" to r.prefix,
                    "weight" to r.weight
                )
            }.sortedByDescending { it["weight"] as Int }

            ctx.json(mapOf("ranks" to ranks))
        }

        // Public: server list with player counts
        app.get("/api/public/servers") { ctx ->
            val servers = brennon.coreServerManager.getServers().filter { it.isOnline }.map { s ->
                mapOf(
                    "name" to s.name,
                    "group" to s.group,
                    "playerCount" to s.playerCount,
                    "maxPlayers" to s.maxPlayers,
                    "online" to s.isOnline
                )
            }
            val totalPlayers = brennon.coreServerManager.getNetworkPlayerCount()
            ctx.json(mapOf(
                "totalPlayers" to totalPlayers,
                "serverCount" to servers.size,
                "servers" to servers
            ))
        }

        // Public: leaderboard for a stat
        app.get("/api/public/leaderboard/{statId}") { ctx ->
            if (!brennon.config.modules.stats) {
                ctx.status(404).json(mapOf("error" to "Stats module is disabled"))
                return@get
            }

            val statId = ctx.pathParam("statId")
            val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 10
            val leaderboard = brennon.coreStatsManager.getLeaderboard(statId, limit).join()
            val entries = leaderboard.entries.mapIndexed { index, (uuid, value) ->
                val name = brennon.corePlayerManager.getCachedPlayer(uuid)?.name ?: uuid.toString()
                mapOf("rank" to index + 1, "uuid" to uuid.toString(), "name" to name, "value" to value)
            }
            ctx.json(mapOf("statId" to statId, "entries" to entries))
        }

        // Public: player profile (limited info)
        app.get("/api/public/player/{uuid}") { ctx ->
            val uuid = try {
                UUID.fromString(ctx.pathParam("uuid"))
            } catch (_: Exception) {
                ctx.status(400).json(mapOf("error" to "Invalid UUID"))
                return@get
            }

            val player = brennon.corePlayerManager.getPlayer(uuid).join()
            if (player.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Player not found"))
                return@get
            }

            val p = player.get()
            ctx.json(mapOf(
                "uuid" to p.uniqueId.toString(),
                "name" to p.name,
                "ranks" to p.getRanks().map { mapOf("id" to it.id, "displayName" to it.displayName) },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "online" to p.isOnline
            ))
        }

        // Public: player profile by name
        app.get("/api/public/player/name/{name}") { ctx ->
            val name = ctx.pathParam("name")
            val player = brennon.corePlayerManager.getPlayer(name).join()
            if (player.isEmpty) {
                ctx.status(404).json(mapOf("error" to "Player not found"))
                return@get
            }

            val p = player.get()
            ctx.json(mapOf(
                "uuid" to p.uniqueId.toString(),
                "name" to p.name,
                "ranks" to p.getRanks().map { mapOf("id" to it.id, "displayName" to it.displayName) },
                "firstJoin" to p.firstJoin.toString(),
                "lastSeen" to p.lastSeen.toString(),
                "online" to p.isOnline
            ))
        }

        // Public: paginated ban list
        app.get("/api/public/bans") { ctx ->
            val limit = (ctx.queryParam("limit")?.toIntOrNull() ?: 20).coerceIn(1, 100)
            val offset = (ctx.queryParam("offset")?.toIntOrNull() ?: 0).coerceAtLeast(0)

            val bans = brennon.databaseManager.punishments
                .findAllByType(PunishmentType.BAN, limit, offset).join()

            val punishmentIds = bans.map { it.id }
            val appeals = appealRepository.getByPunishmentIds(punishmentIds)

            val total = brennon.databaseManager.punishments.countByType(PunishmentType.BAN).join()

            val entries = bans.map { p ->
                val playerName = brennon.corePlayerManager.getCachedPlayer(p.target)?.getName() ?: p.target.toString()
                val appeal = appeals[p.id]
                val status = when {
                    p.revokedBy != null -> "REVOKED"
                    !p.isEffectivelyActive() -> "EXPIRED"
                    else -> "ACTIVE"
                }
                mapOf(
                    "id" to p.id,
                    "playerUuid" to p.target.toString(),
                    "playerName" to playerName,
                    "reason" to p.reason,
                    "issuedAt" to p.issuedAt.toEpochMilli(),
                    "expiresAt" to p.expiresAt?.toEpochMilli(),
                    "isPermanent" to (p.expiresAt == null),
                    "status" to status,
                    "appealStatus" to (appeal?.status?.name ?: "NONE")
                )
            }

            ctx.json(mapOf(
                "total" to total,
                "limit" to limit,
                "offset" to offset,
                "bans" to entries
            ))
        }

        // Public: bans for a specific player
        app.get("/api/public/bans/{uuid}") { ctx ->
            val uuid = try {
                UUID.fromString(ctx.pathParam("uuid"))
            } catch (_: Exception) {
                ctx.status(400).json(mapOf("error" to "Invalid UUID"))
                return@get
            }

            val history = brennon.corePunishmentManager.getHistory(uuid).join()
            val bans = history.filter { it.type == PunishmentType.BAN }

            val punishmentIds = bans.map { it.id }
            val appeals = appealRepository.getByPunishmentIds(punishmentIds)

            val playerName = brennon.corePlayerManager.getCachedPlayer(uuid)?.getName() ?: uuid.toString()

            val entries = bans.map { p ->
                val appeal = appeals[p.id]
                val status = when {
                    p.isRevoked -> "REVOKED"
                    !p.isActive -> "EXPIRED"
                    else -> "ACTIVE"
                }
                mapOf(
                    "id" to p.id,
                    "playerUuid" to uuid.toString(),
                    "playerName" to playerName,
                    "reason" to p.reason,
                    "issuedAt" to p.issuedAt.toEpochMilli(),
                    "expiresAt" to p.expiresAt?.toEpochMilli(),
                    "isPermanent" to p.isPermanent,
                    "status" to status,
                    "appealStatus" to (appeal?.status?.name ?: "NONE")
                )
            }

            ctx.json(mapOf("bans" to entries))
        }
    }
}
