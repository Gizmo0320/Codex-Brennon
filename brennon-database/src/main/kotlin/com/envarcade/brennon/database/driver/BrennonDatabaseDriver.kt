package com.envarcade.brennon.database.driver

import com.envarcade.brennon.common.config.NetworkContext
import com.envarcade.brennon.database.repository.PlayerRepository
import com.envarcade.brennon.database.repository.PunishmentRepository
import com.envarcade.brennon.database.repository.RankRepository
import com.envarcade.brennon.database.repository.StatsRepository
import com.envarcade.brennon.database.repository.TicketRepository

interface BrennonDatabaseDriver {

    fun connect()

    fun disconnect()

    fun isConnected(): Boolean

    fun createPlayerRepository(): PlayerRepository

    fun createRankRepository(): RankRepository

    fun createPunishmentRepository(networkContext: NetworkContext): PunishmentRepository

    fun createTicketRepository(networkContext: NetworkContext): TicketRepository

    fun createStatsRepository(networkContext: NetworkContext): StatsRepository
}
