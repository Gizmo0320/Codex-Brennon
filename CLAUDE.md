# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Brennon Network Core v2.0 is a cross-platform, multi-server Minecraft network core system by Envarcade. It provides a unified API and shared services across an entire distributed server network. The API layer is pure Java (for maximum compatibility); all other modules are Kotlin.

### Supported Platforms

- **Server**: Paper, Folia (region-aware scheduling), Forge, NeoForge, Sponge
- **Proxy**: Velocity
- **Integrations**: Discord (JDA bot/webhooks), Web (SparkJava REST API)
- **Standalone**: CLI application (JLine terminal)

### Feature Systems

All features are defined as Java interfaces in `brennon-api` and implemented as `Core*Manager` classes in `brennon-core`:

- **PlayerManager** — Player lifecycle, data persistence, network-wide sessions (Redis-backed)
- **RankManager** — Rank hierarchy, permissions, rank-based features
- **PunishmentManager** — Bans, mutes, kicks, warns, punishment history
- **EconomyManager** — Currency, balances, transactions
- **ChatManager** — Chat channels, chat filtering
- **TicketManager** — Support tickets with status, priority, and messages
- **StatsManager** — Player statistics tracking
- **ServerManager** — Server info, server grouping, player counts across the network
- **GuiManager** — Inventory-based GUI system (GuiBuilder, GuiItem, click handling)
- **ModuleManager** — Plugin architecture for extending Brennon with custom modules
- **MessagingService** — Cross-server message broker (Redis pub/sub)
- **EventBus** — In-JVM event system for decoupled communication
- **Staff tools** — Staff mode, vanish, staff chat, reports (in brennon-core)

## Build Commands

```bash
./gradlew build        # Build all modules
./gradlew shadowJar    # Build fat JARs for deployable plugins (bukkit, proxy, standalone, etc.)
```

There is no test suite configured. No linting tools beyond `kotlin.code.style=official` in gradle.properties.

## Architecture

### Module Dependency Flow

```
brennon-api (Java interfaces)
    ↓
brennon-common (shared models, config, utilities)
    ↓
brennon-database + brennon-messaging (infrastructure)
    ↓
brennon-core (all Core*Manager implementations, commands, scheduler)
    ↓
Platform modules: brennon-bukkit, brennon-folia, brennon-forge, brennon-neoforge, brennon-sponge
Proxy modules:    brennon-proxy (Velocity)
Integration:      brennon-discord, brennon-web
Standalone:       brennon-standalone
```

### Key Design Patterns

**API-first with Service Locator**: `BrennonAPI` (Java interface) defines all manager accessors. `BrennonProvider` is a static singleton that holds the active `BrennonAPI` instance. External plugins call `BrennonProvider.get()` to access the API.

**Platform abstraction**: The `Brennon` class in brennon-core implements `BrennonAPI` and bootstraps all services. Platform modules (bukkit, proxy, forge, etc.) instantiate `Brennon` with a `Platform` enum and inject platform-specific hooks post-initialization via function properties:
```kotlin
brennon.corePlayerManager.messageSender = { uuid, component -> /* platform-specific send */ }
brennon.coreServerManager.localPlayerCountProvider = { /* platform-specific count */ }
```

**Manager/Repository pattern**: Each subsystem has a `Core*Manager` in brennon-core backed by typed repositories in brennon-database. Async operations return `CompletableFuture`.

**Event system**: `CoreEventBus` handles in-JVM events. Redis pub/sub (via Jedis in brennon-messaging) handles cross-server events using GSON-serialized packets.

**Multi-database**: `DatabaseManager` in brennon-database abstracts over MongoDB and SQL (MySQL/MariaDB/PostgreSQL via HikariCP). Driver selection is config-driven.

### Package Convention

All code lives under `com.envarcade.brennon.<module>.<feature>`.

### Configuration

Runtime config is `config.json` (loaded by `ConfigLoader` from the data folder). Structure is defined in `BrennonConfig` — includes server identity, database credentials, Redis connection, and module toggles.

## Key Files

- `brennon-api/.../BrennonAPI.java` — The public API interface all platforms expose
- `brennon-api/.../BrennonProvider.java` — Static accessor for the API singleton
- `brennon-core/.../Brennon.kt` — Core bootstrap: initializes all managers, registers commands, starts scheduler
- `brennon-core/.../command/CommandRegistry.kt` — Central command dispatcher
- `brennon-database/.../DatabaseManager.kt` — Database driver abstraction
- `brennon-messaging/.../RedisMessagingService.kt` — Redis pub/sub implementation
- `brennon-common/.../config/BrennonConfig.kt` — Configuration data classes

## Language & Build Notes

- Java 17 minimum. `brennon-api` is pure Java; all other modules use Kotlin.
- Kotlin compiler flag: `-Xjsr305=strict` for null-safety interop.
- Shadow plugin used on all platform/integration modules for fat JAR packaging.
- Gradle parallel builds and caching are enabled.
