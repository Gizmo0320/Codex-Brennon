package com.envarcade.brennon.core.economy

import com.envarcade.brennon.api.economy.EconomyManager
import com.envarcade.brennon.core.event.BalanceChangeEvent
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.TransferEvent
import com.envarcade.brennon.core.player.CorePlayerManager
import com.envarcade.brennon.database.DatabaseManager
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Core implementation of the EconomyManager.
 *
 * Handles cross-server economy with database persistence
 * and Redis-based balance sync notifications.
 */
class CoreEconomyManager(
    private val database: DatabaseManager,
    private val playerManager: CorePlayerManager,
    private val messaging: RedisMessagingService,
    private val eventBus: CoreEventBus
) : EconomyManager {

    /** Optional stats tracking — set by Brennon bootstrap when stats module is enabled. */
    var statsTracker: ((UUID, String, Double) -> Unit)? = null

    override fun getBalance(uuid: UUID): CompletableFuture<Double> {
        // Check online cache first
        val cached = playerManager.getCachedPlayer(uuid)
        if (cached != null) {
            return CompletableFuture.completedFuture(cached.getData().balance)
        }

        // Load from database
        return database.players.findByUuid(uuid).thenApply { data ->
            data?.balance ?: 0.0
        }
    }

    override fun setBalance(uuid: UUID, amount: Double): CompletableFuture<Void> {
        require(amount >= 0) { "Balance cannot be negative." }

        return modifyBalance(uuid) { data ->
            val old = data.balance
            data.balance = amount
            eventBus.publish(BalanceChangeEvent(uuid, old, amount, "set"))
        }
    }

    override fun deposit(uuid: UUID, amount: Double): CompletableFuture<Double> {
        require(amount > 0) { "Deposit amount must be positive." }

        return modifyBalanceWithResult(uuid) { data ->
            val old = data.balance
            data.balance += amount
            eventBus.publish(BalanceChangeEvent(uuid, old, data.balance, "deposit"))
            statsTracker?.invoke(uuid, com.envarcade.brennon.api.stats.StatTypes.MONEY_EARNED, amount)
            data.balance
        }
    }

    override fun withdraw(uuid: UUID, amount: Double): CompletableFuture<Double> {
        require(amount > 0) { "Withdrawal amount must be positive." }

        return modifyBalanceWithResult(uuid) { data ->
            if (data.balance < amount) {
                throw IllegalStateException("Insufficient funds. Balance: ${data.balance}, Requested: $amount")
            }
            val old = data.balance
            data.balance -= amount
            eventBus.publish(BalanceChangeEvent(uuid, old, data.balance, "withdraw"))
            statsTracker?.invoke(uuid, com.envarcade.brennon.api.stats.StatTypes.MONEY_SPENT, amount)
            data.balance
        }
    }

    override fun transfer(from: UUID, to: UUID, amount: Double): CompletableFuture<Void> {
        require(amount > 0) { "Transfer amount must be positive." }
        require(from != to) { "Cannot transfer to yourself." }

        // Withdraw from sender, then deposit to receiver
        return withdraw(from, amount).thenCompose {
            deposit(to, amount)
        }.thenRun {
            eventBus.publish(TransferEvent(from, to, amount))
            messaging.publish(Channels.ECONOMY_UPDATE, """
                {"from":"$from","to":"$to","amount":$amount,"type":"transfer"}
            """.trimIndent())
        }
    }

    override fun has(uuid: UUID, amount: Double): CompletableFuture<Boolean> {
        return getBalance(uuid).thenApply { balance -> balance >= amount }
    }

    // ============================================================
    // Internal Helpers
    // ============================================================

    /**
     * Modifies a player's balance atomically (cached or database).
     */
    private fun modifyBalance(uuid: UUID, modifier: (com.envarcade.brennon.common.model.PlayerData) -> Unit): CompletableFuture<Void> {
        // If online, modify cached data and save
        val cached = playerManager.getCachedPlayer(uuid)
        if (cached != null) {
            cached.updateData(modifier)
            return database.players.save(cached.getData()).thenRun {
                notifyBalanceChange(uuid)
            }
        }

        // If offline, load -> modify -> save
        return database.players.findByUuid(uuid).thenCompose { data ->
            if (data == null) {
                CompletableFuture.failedFuture(IllegalArgumentException("Player $uuid not found."))
            } else {
                modifier(data)
                database.players.save(data).thenRun {
                    notifyBalanceChange(uuid)
                }
            }
        }
    }

    /**
     * Modifies a player's balance and returns the new balance.
     */
    private fun modifyBalanceWithResult(uuid: UUID, modifier: (com.envarcade.brennon.common.model.PlayerData) -> Double): CompletableFuture<Double> {
        val cached = playerManager.getCachedPlayer(uuid)
        if (cached != null) {
            val result = modifier(cached.getData())
            return database.players.save(cached.getData()).thenApply {
                notifyBalanceChange(uuid)
                result
            }
        }

        return database.players.findByUuid(uuid).thenCompose { data ->
            if (data == null) {
                CompletableFuture.failedFuture(IllegalArgumentException("Player $uuid not found."))
            } else {
                val result = modifier(data)
                database.players.save(data).thenApply {
                    notifyBalanceChange(uuid)
                    result
                }
            }
        }
    }

    /**
     * Notifies other servers of a balance change via Redis.
     */
    private fun notifyBalanceChange(uuid: UUID) {
        try {
            messaging.publish(Channels.ECONOMY_UPDATE, """
                {"uuid":"$uuid","type":"balance_change"}
            """.trimIndent())
        } catch (e: Exception) {
            println("[Brennon] Failed to publish economy update for $uuid: ${e.message}")
        }
    }
}
