# Brennon 2.0

Brennon is a comprehensive Minecraft network management system. It provides a unified API for player management, ranks, economy, punishments, chat, tickets, statistics, staff tools, server registry, and GUI menus — all synchronized across multiple servers via Redis.

## Features

- **Player Management** — Track players across the network, first join, last seen, current server
- **Ranks** — Hierarchical rank system with permissions, prefix, weight, inheritance, and staff flag
- **Economy** — Balance, deposit, withdraw, transfer, and balance checks
- **Punishments** — Ban, mute, kick, warn with duration support and full history
- **Chat System** — Cross-server channels (global, staff, trade, local), private messaging, chat filters
- **Tickets** — Support ticket system with creation, replies, assignment, priority, and status tracking
- **Statistics** — 20 built-in stat types (playtime, kills, deaths, KDR, economy, blocks, chat, punishments, reports, tickets) with leaderboards
- **Staff Tools** — Staff mode, vanish, staff chat, staff list, player reports
- **Server Registry** — Dynamic server registration with groups, fallback servers, and access control
- **GUI System** — Cross-platform inventory menu framework (builder-style API)
- **Web Dashboard** — Full-stack web dashboard with REST API, WebSocket real-time events, and Vue 3 frontend

## Modules

| Module | Description |
|--------|-------------|
| `brennon-api` | Java API interfaces — the public contract for all systems |
| `brennon-common` | Shared models, config classes, and utilities |
| `brennon-database` | Database repositories for MongoDB, MySQL, MariaDB, PostgreSQL |
| `brennon-messaging` | Redis pub/sub messaging service and channel definitions |
| `brennon-core` | Core implementation of all managers, commands, and events |
| `brennon-bukkit` | Paper/Spigot platform module |
| `brennon-folia` | Folia platform module (region-threaded scheduling) |
| `brennon-forge` | Minecraft Forge platform module |
| `brennon-neoforge` | NeoForge platform module |
| `brennon-sponge` | Sponge platform module |
| `brennon-proxy` | Velocity proxy platform module |
| `brennon-discord` | Discord bot integration (JDA) |
| `brennon-web` | Lightweight embedded web API module |
| `brennon-standalone` | Standalone headless process (no Minecraft server) |
| `brennon-webserver` | Full-stack web dashboard (Javalin + Vue 3) |

## Supported Platforms

| Platform | Type | Notes |
|----------|------|-------|
| Paper / Spigot | Server | Primary server platform |
| Folia | Server | Region-threaded Paper fork — uses Folia scheduler |
| Minecraft Forge | Server | 1.20.x — uses GsonComponentSerializer for Adventure |
| NeoForge | Server | 1.20.x — uses GsonComponentSerializer for Adventure |
| Sponge | Server | Native Adventure support |
| Velocity | Proxy | Proxy-layer features (server switching, network chat) |
| Discord | Bot | Chat sync, punishment alerts, player count status |
| Standalone | Headless | Runs without a Minecraft server (for web dashboard, scripts) |

## Requirements

- **Java 17** or newer
- **Redis** server (for cross-server messaging)
- **Database**: One of MongoDB, MySQL, MariaDB, or PostgreSQL
- **Gradle 8.5** (included via wrapper)

## Quick Start

1. Build the project:
   ```bash
   ./gradlew shadowJar
   ```

2. Copy the appropriate JAR to your server's plugins folder:
   - Paper/Spigot: `brennon-bukkit/build/libs/brennon-bukkit-2.0.0-SNAPSHOT-all.jar`
   - Velocity: `brennon-proxy/build/libs/brennon-proxy-2.0.0-SNAPSHOT-all.jar`
   - Forge: `brennon-forge/build/libs/brennon-forge-2.0.0-SNAPSHOT-all.jar`

3. Start the server once to generate the default `brennon.json` config file.

4. Edit `brennon.json` to configure your database and Redis connection (see [CONFIGURATION.md](CONFIGURATION.md)).

5. Restart the server.

### Web Dashboard

To run the web dashboard as a standalone process:

```bash
java -jar brennon-webserver/build/libs/brennon-webserver-2.0.0-SNAPSHOT-all.jar [data-folder]
```

On first run, it creates a `webserver.json` config file. Edit it to set your API key, JWT secret, and dashboard user credentials.

## Documentation

- [Configuration Reference](CONFIGURATION.md) — All config fields for `brennon.json` and `webserver.json`
- [Command Reference](COMMANDS.md) — All 24 in-game commands
- [Developer API](API.md) — Java/Kotlin API interfaces, events, and code examples
- [REST API](REST_API.md) — 50+ HTTP endpoints and WebSocket documentation
- [Platform Setup](PLATFORMS.md) — Per-platform installation and setup guides
- [Architecture](ARCHITECTURE.md) — Module structure, design patterns, and extending guides
