package com.envarcade.brennon.messaging.redis

import com.envarcade.brennon.api.messaging.MessageHandler
import com.envarcade.brennon.api.messaging.MessagingService
import com.envarcade.brennon.common.config.RedisConfig
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub
import redis.clients.jedis.params.SetParams
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RedisMessagingService(private val config: RedisConfig) : MessagingService {

    private lateinit var jedisPool: JedisPool
    private val subscribers = ConcurrentHashMap<String, JedisPubSub>()
    private val handlers = ConcurrentHashMap<String, MessageHandler>()
    private val executor: ExecutorService = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "Brennon-Redis-Sub").apply { isDaemon = true }
    }

    fun initialize() {
        val poolConfig = JedisPoolConfig().apply {
            maxTotal = config.poolSize
            maxIdle = config.poolSize / 2
            minIdle = 1
            testOnBorrow = true
            testWhileIdle = true
        }

        jedisPool = if (config.password.isNotBlank()) {
            JedisPool(poolConfig, config.host, config.port, config.timeout, config.password, config.database)
        } else {
            JedisPool(poolConfig, config.host, config.port, config.timeout, null, config.database)
        }

        jedisPool.resource.use { it.ping() }
        println("[Brennon] Connected to Redis: ${config.host}:${config.port}")
    }

    fun shutdown() {
        println("[Brennon] Shutting down Redis messaging...")
        subscribers.values.forEach { it.unsubscribe() }
        subscribers.clear()
        handlers.clear()
        executor.shutdownNow()
        jedisPool.close()
    }

    override fun publish(channel: String, message: String) {
        val prefixedChannel = config.channelPrefix + channel
        jedisPool.resource.use { jedis ->
            jedis.publish(prefixedChannel, message)
        }
    }

    override fun subscribe(channel: String, handler: MessageHandler) {
        val prefixedChannel = config.channelPrefix + channel
        handlers[prefixedChannel] = handler

        val pubSub = object : JedisPubSub() {
            override fun onMessage(ch: String, message: String) {
                handlers[ch]?.onMessage(channel, message)
            }
        }

        subscribers[prefixedChannel] = pubSub

        executor.submit {
            try {
                jedisPool.resource.use { jedis ->
                    jedis.subscribe(pubSub, prefixedChannel)
                }
            } catch (e: Exception) {
                if (!executor.isShutdown) {
                    println("[Brennon] Redis subscription error on $channel: ${e.message}")
                }
            }
        }
    }

    override fun unsubscribe(channel: String) {
        val prefixedChannel = config.channelPrefix + channel
        subscribers.remove(prefixedChannel)?.unsubscribe()
        handlers.remove(prefixedChannel)
    }

    override fun isConnected(): Boolean {
        return try {
            jedisPool.resource.use { it.ping() == "PONG" }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Attempts to acquire a distributed lock using Redis SETNX.
     * Returns true if the lock was acquired, false if another holder already has it.
     */
    fun tryLock(key: String, ttlSeconds: Int): Boolean {
        val prefixedKey = config.channelPrefix + key
        return jedisPool.resource.use { jedis ->
            val result = jedis.set(prefixedKey, "1", SetParams().nx().ex(ttlSeconds.toLong()))
            result == "OK"
        }
    }

    fun getPool(): JedisPool = jedisPool
}
