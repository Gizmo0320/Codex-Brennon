# Architecture Guide

This document covers the internal architecture, design patterns, and extension points of Brennon 2.0.

## Module Dependency Graph

```
brennon-api          (Java interfaces — no dependencies)
    |
brennon-common       (models, config, utilities)
    |
brennon-database     (MongoDB + SQL repositories)
    |
brennon-messaging    (Redis pub/sub, channel definitions)
    |
brennon-core         (manager implementations, commands, events)
    |
    +-- brennon-bukkit      (Paper/Spigot)
    +-- brennon-folia       (Folia)
    +-- brennon-forge       (Minecraft Forge)
    +-- brennon-neoforge    (NeoForge)
    +-- brennon-sponge      (Sponge)
    +-- brennon-proxy       (Velocity)
    +-- brennon-discord     (JDA)
    +-- brennon-web         (Embedded web API)
    +-- brennon-standalone  (Headless)
    +-- brennon-webserver   (Full-stack dashboard)
```

Every platform module depends on `brennon-core`, which pulls in all lower layers.

## Design Patterns

### API-First

All public contracts are Java interfaces in `brennon-api`. Implementations are in Kotlin in `brennon-core`. This ensures:
- Java compatibility for third-party plugins
- Clear separation between contract and implementation
- Platform modules only need to know about the API

### Platform Hooks

Platform-specific behavior is injected via function properties:

```kotlin
// In Brennon.kt (core)
var messageSender: ((UUID, Component) -> Unit)? = null
var playerKicker: ((UUID, Component) -> Unit)? = null
var permissionChecker: ((UUID, String) -> Boolean)? = null
```

Each platform module sets these during initialization:

```kotlin
// In BrennonBukkit
brennon.messageSender = { uuid, component ->
    Bukkit.getPlayer(uuid)?.sendMessage(component)
}
```

### CompletableFuture

All database and async operations return `CompletableFuture<T>`. This allows:
- Non-blocking calls from any thread
- Easy chaining with `.thenAccept()`, `.thenCompose()`
- Platform modules can bridge to their own async systems

### ConcurrentHashMap Caching

In-memory caches use `ConcurrentHashMap` for thread-safe concurrent access:

```kotlin
private val playerCache = ConcurrentHashMap<UUID, NetworkPlayerImpl>()
private val rankCache = ConcurrentHashMap<String, RankImpl>()
```

### lateinit Managers

Core managers use `lateinit var` with `private set`:

```kotlin
lateinit var corePlayerManager: CorePlayerManager
    private set
lateinit var coreRankManager: CoreRankManager
    private set
```

Managers are initialized during `Brennon.enable()` based on module flags in config. External code should check `config.modules.chat` (etc.) before accessing optional managers. The `::prop.isInitialized` check only works from within the owning class.

---

## Redis Channels

All cross-server communication goes through Redis pub/sub. Channel names are defined in `Channels.kt`:

| Constant | Channel | Description |
|----------|---------|-------------|
| `PLAYER_JOIN` | `player:join` | Player joined the network |
| `PLAYER_QUIT` | `player:quit` | Player left the network |
| `PLAYER_SWITCH` | `player:switch` | Player switched servers |
| `PUNISHMENT_ISSUED` | `punishment:issued` | Punishment created |
| `PUNISHMENT_REVOKED` | `punishment:revoked` | Punishment revoked |
| `RANK_UPDATE` | `rank:update` | Player rank changed |
| `ECONOMY_UPDATE` | `economy:update` | Balance changed |
| `SERVER_STATUS` | `server:status` | Server heartbeat/status |
| `STAFF_CHAT` | `staff:chat` | Staff chat message |
| `STAFF_ALERT` | `staff:alert` | Staff alert (report, etc.) |
| `BROADCAST` | `broadcast` | Network-wide broadcast |
| `COMMAND_SYNC` | `command:sync` | Cross-server command execution |
| `CHAT_MESSAGE` | `chat:message` | Public chat message |
| `CHAT_PRIVATE` | `chat:private` | Private message |
| `TICKET_CREATE` | `ticket:create` | Ticket created |
| `TICKET_UPDATE` | `ticket:update` | Ticket status changed |
| `TICKET_REPLY` | `ticket:reply` | Ticket reply added |
| `STAT_UPDATE` | `stat:update` | Stat value changed |
| `SERVER_REGISTRY_UPDATE` | `server:registry:update` | Server registered/unregistered |
| `SERVER_GROUP_UPDATE` | `server:group:update` | Server group created/deleted |

All channels are prefixed with the configured `channelPrefix` (default: `brennon:`).

Network-scoped variants exist for chat: `chat:<networkId>:message` and `chat:<networkId>:private`.

---

## Database Schema

### MongoDB Collections

| Collection | Description |
|-----------|-------------|
| `players` | Player data (UUID, name, ranks, firstJoin, lastSeen) |
| `ranks` | Rank definitions (id, displayName, prefix, weight, permissions) |
| `punishments` | Punishment records (type, target, issuer, reason, duration, active) |
| `economy` | Player balances |
| `tickets` | Support tickets with embedded messages array |
| `stats` | Player statistics (UUID → stat map) |
| `servers` | Server registry definitions |
| `server_groups` | Server group definitions |

### SQL Tables

Same structure as MongoDB collections, mapped to relational tables. The database module handles differences between MySQL and PostgreSQL (e.g., upsert syntax: `ON DUPLICATE KEY UPDATE` vs `ON CONFLICT DO UPDATE`).

---

## Extending Brennon

### Adding a New Platform Module

1. Create a new Gradle submodule (e.g., `brennon-myplatform`)
2. Add it to `settings.gradle.kts`
3. Add a project block in `build.gradle.kts` with platform-specific dependencies
4. Create a main class that:
   - Creates a `Brennon(Platform.PAPER, dataFolder)` instance (or appropriate platform)
   - Calls `brennon.enable()` on startup
   - Sets platform hooks (`messageSender`, `playerKicker`, `permissionChecker`)
   - Registers commands with the platform's command system
   - Hooks into platform events (chat, join, quit, etc.)
   - Calls `brennon.disable()` on shutdown

### Adding a New Command

1. Create a class in `brennon-core/.../command/impl/` extending `BrennonCommand`
2. Implement `execute()` and optionally `tabComplete()`
3. Register it in `Brennon.kt`'s command setup section
4. The command will automatically work on all platforms

```kotlin
class MyCommand(private val brennon: Brennon) : BrennonCommand(
    name = "mycommand",
    permission = "brennon.command.mycommand",
    aliases = listOf("mc"),
    usage = "/mycommand <args>",
    description = "Does something"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        // Implementation
    }
}
```

### Adding a New REST API Endpoint

1. Create a route class in `brennon-webserver/.../routes/`:

```kotlin
class MyRoutes(private val brennon: Brennon) {
    fun register(app: Javalin) {
        app.get("/api/my-endpoint") { ctx ->
            ctx.json(mapOf("hello" to "world"))
        }
    }
}
```

2. Register it in `BrennonWebServer.kt`:

```kotlin
MyRoutes(brennon).register(app)
```

### Adding a New Event

1. Create an event class in `brennon-core/.../event/Events.kt`:

```kotlin
class MyCustomEvent(
    val someField: String
) : BrennonEvent()
```

2. Publish it where appropriate:

```kotlin
brennon.eventBus.publish(MyCustomEvent("value"))
```

3. Subscribe from any module:

```kotlin
brennon.eventBus.subscribe(MyCustomEvent::class.java) { event ->
    println(event.someField)
}
```

### Adding a New Stat Type

Stats are string-based IDs. To add a new built-in stat:

1. Add a constant to `StatTypes.java`:
   ```java
   public static final String MY_STAT = "my_stat";
   ```

2. Increment it where appropriate:
   ```kotlin
   brennon.coreStatsManager.incrementStat(playerUuid, StatTypes.MY_STAT, 1.0)
   ```

Custom stats don't require code changes — any string ID works with `incrementStat()` / `setStat()`.
