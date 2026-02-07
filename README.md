# Brennon Network Core v2.0

A comprehensive, multi-platform Minecraft network core system built with Java and Kotlin.

## Architecture

```
brennon/
├── brennon-api/            # Public Java API (other plugins depend on this)
├── brennon-common/         # Shared utilities, models, configs (Kotlin)
├── brennon-core/           # Core logic and service implementations (Kotlin)
├── brennon-database/       # Multi-driver database abstraction (Kotlin)
├── brennon-messaging/      # Redis pub/sub cross-server messaging (Kotlin)
├── brennon-proxy/          # Velocity proxy plugin (Kotlin)
├── brennon-bukkit/         # Paper/Spigot server plugin (Kotlin)
└── brennon-standalone/     # Standalone application (Kotlin)
```

## Tech Stack

- **Java 17+** — Public API for maximum compatibility
- **Kotlin** — Core logic, utilities, and platform modules
- **Gradle (Kotlin DSL)** — Multi-module build system
- **Redis (Jedis)** — Cross-server messaging & caching
- **MongoDB / MySQL / PostgreSQL** — Multi-driver database support
- **Adventure API** — Cross-platform text components (MiniMessage)
- **HikariCP** — SQL connection pooling
- **Shadow** — Fat JAR packaging for deployable plugins

## Building

```bash
./gradlew build        # Build all modules
./gradlew shadowJar    # Build fat JARs for proxy/bukkit/standalone
```

## API Usage

```java
BrennonAPI api = BrennonProvider.get();
api.getPlayerManager().getPlayer(uuid).thenAccept(player -> {
    player.ifPresent(p -> p.sendMessage(Component.text("Hello!")));
});
```

---

**Envarcade** — Brennon Network Core
