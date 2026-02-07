package com.envarcade.brennon.core.server

import com.envarcade.brennon.common.model.ServerDefinition
import com.envarcade.brennon.common.model.ServerGroupDefinition
import com.envarcade.brennon.core.event.CoreEventBus
import com.envarcade.brennon.core.event.ServerGroupCreatedEvent
import com.envarcade.brennon.core.event.ServerGroupDeletedEvent
import com.envarcade.brennon.core.event.ServerRegisteredEvent
import com.envarcade.brennon.core.event.ServerUnregisteredEvent
import com.envarcade.brennon.messaging.channel.Channels
import com.envarcade.brennon.messaging.redis.RedisMessagingService
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis-backed server registry for dynamic server management.
 *
 * Stores server definitions and group definitions in Redis hashes
 * with index sets for fast iteration. Changes are broadcast via
 * pub/sub so multiple proxies stay in sync.
 */
class ServerRegistryService(
    private val messaging: RedisMessagingService,
    private val eventBus: CoreEventBus
) {

    private val serverCache = ConcurrentHashMap<String, ServerDefinition>()
    private val groupCache = ConcurrentHashMap<String, ServerGroupDefinition>()
    private val gson = Gson()

    companion object {
        private const val SERVER_KEY_PREFIX = "brennon:registry:server:"
        private const val SERVER_SET_KEY = "brennon:registry:servers"
        private const val GROUP_KEY_PREFIX = "brennon:registry:group:"
        private const val GROUP_SET_KEY = "brennon:registry:groups"
        private fun groupServersKey(groupId: String) = "brennon:registry:group:$groupId:servers"
    }

    fun initialize() {
        loadAllFromRedis()
        subscribeToChanges()
        println("[Brennon] Server registry initialized: ${serverCache.size} servers, ${groupCache.size} groups.")
    }

    fun shutdown() {
        messaging.unsubscribe(Channels.SERVER_REGISTRY_UPDATE)
        messaging.unsubscribe(Channels.SERVER_GROUP_UPDATE)
    }

    // ============================================================
    // Server CRUD
    // ============================================================

    fun registerServer(def: ServerDefinition): Boolean {
        if (serverCache.containsKey(def.name)) return false

        serverCache[def.name] = def
        saveServerToRedis(def)
        publishChange(Channels.SERVER_REGISTRY_UPDATE, gson.toJson(mapOf(
            "action" to "register",
            "serverName" to def.name
        )))
        eventBus.publish(ServerRegisteredEvent(def.name, def.group, def.host, def.port, def.autoRegistered))
        println("[Brennon] Server registered: ${def.name} (${def.address}) in group '${def.group}'")
        return true
    }

    fun unregisterServer(name: String): Boolean {
        val def = serverCache.remove(name) ?: return false
        removeServerFromRedis(name, def.group)
        publishChange(Channels.SERVER_REGISTRY_UPDATE, gson.toJson(mapOf(
            "action" to "unregister",
            "serverName" to name
        )))
        eventBus.publish(ServerUnregisteredEvent(name, def.group))
        println("[Brennon] Server unregistered: $name")
        return true
    }

    fun getServerDefinition(name: String): ServerDefinition? = serverCache[name]

    fun getAllServerDefinitions(): Collection<ServerDefinition> = serverCache.values.toList()

    fun getServerDefinitionsByGroup(group: String): Collection<ServerDefinition> =
        serverCache.values.filter { it.group == group }

    fun updateServerGroup(name: String, newGroup: String): Boolean {
        val def = serverCache[name] ?: return false
        val oldGroup = def.group
        val updated = def.copy(group = newGroup)
        serverCache[name] = updated

        try {
            messaging.getPool().resource.use { jedis ->
                jedis.hset("$SERVER_KEY_PREFIX$name", "group", newGroup)
                jedis.srem(groupServersKey(oldGroup), name)
                jedis.sadd(groupServersKey(newGroup), name)
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to update server group: ${e.message}")
        }

        publishChange(Channels.SERVER_REGISTRY_UPDATE, gson.toJson(mapOf(
            "action" to "update",
            "serverName" to name
        )))
        return true
    }

    // ============================================================
    // Group CRUD
    // ============================================================

    fun createGroup(group: ServerGroupDefinition): Boolean {
        if (groupCache.containsKey(group.id)) return false

        groupCache[group.id] = group
        saveGroupToRedis(group)
        publishChange(Channels.SERVER_GROUP_UPDATE, gson.toJson(mapOf(
            "action" to "create",
            "groupId" to group.id
        )))
        eventBus.publish(ServerGroupCreatedEvent(group.id, group.displayName))
        println("[Brennon] Server group created: ${group.id} (${group.displayName})")
        return true
    }

    fun deleteGroup(groupId: String): Boolean {
        val serversInGroup = getServerDefinitionsByGroup(groupId)
        if (serversInGroup.isNotEmpty()) return false

        groupCache.remove(groupId) ?: return false
        removeGroupFromRedis(groupId)
        publishChange(Channels.SERVER_GROUP_UPDATE, gson.toJson(mapOf(
            "action" to "delete",
            "groupId" to groupId
        )))
        eventBus.publish(ServerGroupDeletedEvent(groupId))
        println("[Brennon] Server group deleted: $groupId")
        return true
    }

    fun updateGroup(group: ServerGroupDefinition): Boolean {
        if (!groupCache.containsKey(group.id)) return false

        groupCache[group.id] = group
        saveGroupToRedis(group)
        publishChange(Channels.SERVER_GROUP_UPDATE, gson.toJson(mapOf(
            "action" to "update",
            "groupId" to group.id
        )))
        return true
    }

    fun getGroupDefinition(groupId: String): ServerGroupDefinition? = groupCache[groupId]

    fun getAllGroupDefinitions(): Collection<ServerGroupDefinition> = groupCache.values.toList()

    // ============================================================
    // Internal — Redis I/O
    // ============================================================

    private fun loadAllFromRedis() {
        try {
            messaging.getPool().resource.use { jedis ->
                // Load servers
                val serverNames = jedis.smembers(SERVER_SET_KEY) ?: emptySet()
                for (name in serverNames) {
                    val data = jedis.hgetAll("$SERVER_KEY_PREFIX$name")
                    if (data.isNotEmpty()) {
                        serverCache[name] = serverFromRedis(name, data)
                    }
                }

                // Load groups
                val groupIds = jedis.smembers(GROUP_SET_KEY) ?: emptySet()
                for (id in groupIds) {
                    val data = jedis.hgetAll("$GROUP_KEY_PREFIX$id")
                    if (data.isNotEmpty()) {
                        groupCache[id] = groupFromRedis(id, data)
                    }
                }
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to load server registry from Redis: ${e.message}")
        }
    }

    private fun saveServerToRedis(def: ServerDefinition) {
        try {
            messaging.getPool().resource.use { jedis ->
                jedis.hset("$SERVER_KEY_PREFIX${def.name}", mapOf(
                    "name" to def.name,
                    "group" to def.group,
                    "host" to def.host,
                    "port" to def.port.toString(),
                    "maxPlayers" to def.maxPlayers.toString(),
                    "restricted" to def.restricted.toString(),
                    "permission" to def.permission,
                    "autoRegistered" to def.autoRegistered.toString(),
                    "addedBy" to def.addedBy,
                    "addedAt" to def.addedAt.toString()
                ))
                jedis.sadd(SERVER_SET_KEY, def.name)
                jedis.sadd(groupServersKey(def.group), def.name)
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to save server to Redis: ${e.message}")
        }
    }

    private fun removeServerFromRedis(name: String, group: String) {
        try {
            messaging.getPool().resource.use { jedis ->
                jedis.del("$SERVER_KEY_PREFIX$name")
                jedis.srem(SERVER_SET_KEY, name)
                jedis.srem(groupServersKey(group), name)
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to remove server from Redis: ${e.message}")
        }
    }

    private fun saveGroupToRedis(group: ServerGroupDefinition) {
        try {
            messaging.getPool().resource.use { jedis ->
                jedis.hset("$GROUP_KEY_PREFIX${group.id}", mapOf(
                    "id" to group.id,
                    "displayName" to group.displayName,
                    "joinPriority" to group.joinPriority.toString(),
                    "restricted" to group.restricted.toString(),
                    "permission" to group.permission,
                    "isFallback" to group.isFallback.toString(),
                    "maxPlayers" to group.maxPlayers.toString()
                ))
                jedis.sadd(GROUP_SET_KEY, group.id)
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to save group to Redis: ${e.message}")
        }
    }

    private fun removeGroupFromRedis(groupId: String) {
        try {
            messaging.getPool().resource.use { jedis ->
                jedis.del("$GROUP_KEY_PREFIX$groupId")
                jedis.srem(GROUP_SET_KEY, groupId)
                jedis.del(groupServersKey(groupId))
            }
        } catch (e: Exception) {
            println("[Brennon] Failed to remove group from Redis: ${e.message}")
        }
    }

    private fun publishChange(channel: String, message: String) {
        try {
            messaging.publish(channel, message)
        } catch (e: Exception) {
            println("[Brennon] Failed to publish registry change: ${e.message}")
        }
    }

    private fun subscribeToChanges() {
        messaging.subscribe(Channels.SERVER_REGISTRY_UPDATE) { _, message ->
            try {
                val json = gson.fromJson(message, JsonObject::class.java)
                val action = json.get("action").asString
                val serverName = json.get("serverName").asString

                when (action) {
                    "register", "update" -> {
                        // Re-fetch from Redis
                        messaging.getPool().resource.use { jedis ->
                            val data = jedis.hgetAll("$SERVER_KEY_PREFIX$serverName")
                            if (data.isNotEmpty()) {
                                val def = serverFromRedis(serverName, data)
                                val isNew = !serverCache.containsKey(serverName)
                                serverCache[serverName] = def
                                if (isNew) {
                                    eventBus.publish(ServerRegisteredEvent(def.name, def.group, def.host, def.port, def.autoRegistered))
                                }
                            }
                        }
                    }
                    "unregister" -> {
                        val removed = serverCache.remove(serverName)
                        if (removed != null) {
                            eventBus.publish(ServerUnregisteredEvent(serverName, removed.group))
                        }
                    }
                }
            } catch (e: Exception) {
                println("[Brennon] Error processing server registry update: ${e.message}")
            }
        }

        messaging.subscribe(Channels.SERVER_GROUP_UPDATE) { _, message ->
            try {
                val json = gson.fromJson(message, JsonObject::class.java)
                val action = json.get("action").asString
                val groupId = json.get("groupId").asString

                when (action) {
                    "create", "update" -> {
                        messaging.getPool().resource.use { jedis ->
                            val data = jedis.hgetAll("$GROUP_KEY_PREFIX$groupId")
                            if (data.isNotEmpty()) {
                                val group = groupFromRedis(groupId, data)
                                val isNew = !groupCache.containsKey(groupId)
                                groupCache[groupId] = group
                                if (isNew) {
                                    eventBus.publish(ServerGroupCreatedEvent(group.id, group.displayName))
                                }
                            }
                        }
                    }
                    "delete" -> {
                        val removed = groupCache.remove(groupId)
                        if (removed != null) {
                            eventBus.publish(ServerGroupDeletedEvent(groupId))
                        }
                    }
                }
            } catch (e: Exception) {
                println("[Brennon] Error processing server group update: ${e.message}")
            }
        }
    }

    private fun serverFromRedis(name: String, data: Map<String, String>): ServerDefinition {
        return ServerDefinition(
            name = name,
            group = data["group"] ?: "default",
            host = data["host"] ?: "localhost",
            port = data["port"]?.toIntOrNull() ?: 25565,
            maxPlayers = data["maxPlayers"]?.toIntOrNull() ?: 100,
            restricted = data["restricted"]?.toBooleanStrictOrNull() ?: false,
            permission = data["permission"] ?: "",
            autoRegistered = data["autoRegistered"]?.toBooleanStrictOrNull() ?: false,
            addedBy = data["addedBy"] ?: "system",
            addedAt = data["addedAt"]?.toLongOrNull() ?: 0
        )
    }

    private fun groupFromRedis(id: String, data: Map<String, String>): ServerGroupDefinition {
        return ServerGroupDefinition(
            id = id,
            displayName = data["displayName"] ?: id,
            joinPriority = data["joinPriority"]?.toIntOrNull() ?: 0,
            restricted = data["restricted"]?.toBooleanStrictOrNull() ?: false,
            permission = data["permission"] ?: "",
            isFallback = data["isFallback"]?.toBooleanStrictOrNull() ?: false,
            maxPlayers = data["maxPlayers"]?.toIntOrNull() ?: -1
        )
    }
}
