# Platform Setup Guides

Brennon supports multiple Minecraft server platforms, a proxy, Discord, and standalone deployment. Each platform has its own module with platform-specific hooks.

## Paper / Spigot

**Module:** `brennon-bukkit`

### Installation

1. Build: `./gradlew :brennon-bukkit:shadowJar`
2. Copy `brennon-bukkit/build/libs/brennon-bukkit-2.0.0-SNAPSHOT-all.jar` to your `plugins/` folder
3. Start the server to generate `plugins/Brennon/brennon.json`
4. Configure database and Redis in `brennon.json`
5. Restart

### Notes

- Supports Paper 1.20+ and Spigot (Paper recommended)
- Native Adventure text support (no serialization needed)
- Commands are registered as Bukkit commands with tab completion
- GUI system uses Bukkit inventory API
- Chat events hook into `AsyncPlayerChatEvent`
- Stats auto-tracking: kills, deaths, blocks placed/broken via Bukkit events

---

## Folia

**Module:** `brennon-folia`

### Installation

Same as Paper — copy the Folia JAR instead:

1. Build: `./gradlew :brennon-folia:shadowJar`
2. Copy `brennon-folia/build/libs/brennon-folia-2.0.0-SNAPSHOT-all.jar` to `plugins/`

### Notes

- Uses Folia's region-threaded scheduler (`getServer().getRegionScheduler()`, `getServer().getGlobalRegionScheduler()`)
- All scheduled tasks use Folia-aware scheduling instead of `BukkitScheduler`
- Same feature set as Paper — different scheduling backend
- Do **not** run the Bukkit module on a Folia server (it will crash due to incompatible scheduler calls)

---

## Minecraft Forge

**Module:** `brennon-forge`

### Installation

1. Build: `./gradlew :brennon-forge:shadowJar`
2. Copy the shadow JAR to your Forge server's `mods/` folder
3. Start the server to generate config in `config/brennon/brennon.json`
4. Configure and restart

### Build Notes

- Uses ForgeGradle with `shade` configuration
- Shadow JAR is processed by `reobf` for proper obfuscation mapping
- Targets Minecraft 1.20.1, Forge 47.2.0

### Notes

- Adventure components are converted to Minecraft components via `GsonComponentSerializer`
- Commands registered via Forge's `RegisterCommandsEvent`
- Chat events hook into `ServerChatEvent`
- GUI system uses Forge container/menu system
- Use `!== null` for Minecraft type null checks (Kotlin interop)

---

## NeoForge

**Module:** `brennon-neoforge`

### Installation

1. Build: `./gradlew :brennon-neoforge:shadowJar`
2. Copy to `mods/` folder
3. Configure `config/brennon/brennon.json`

### Build Notes

- Uses NeoGradle — requires `java { toolchain }` instead of `sourceCompatibility`
- Top-level `runs {}` block (different from ForgeGradle)
- Targets NeoForge 20.4+

### Notes

- Same Adventure → MC component conversion as Forge via `GsonComponentSerializer`
- NeoForge event bus for command registration and chat handling
- Similar to Forge module but adapted for NeoForge's event system

---

## Sponge

**Module:** `brennon-sponge`

### Installation

1. Build: `./gradlew :brennon-sponge:shadowJar`
2. Copy to Sponge server's `mods/` folder
3. Configure `config/brennon/brennon.json`

### Notes

- **Native Adventure support** — Sponge uses Adventure natively, no serialization needed
- Commands registered via Sponge's `RegisterCommandEvent`
- Uses Sponge's `Audience` API for sending messages
- Chat events via Sponge's `PlayerChatEvent`

---

## Velocity

**Module:** `brennon-proxy`

### Installation

1. Build: `./gradlew :brennon-proxy:shadowJar`
2. Copy `brennon-proxy/build/libs/brennon-proxy-2.0.0-SNAPSHOT-all.jar` to Velocity's `plugins/` folder
3. Configure `plugins/brennon/brennon.json`

### Notes

- Runs as a Velocity plugin on the proxy layer
- Handles network-level features: player routing (`/server`), cross-server chat, network-wide punishments
- Native Adventure support (Velocity uses Adventure)
- Listens to `LoginEvent` for ban checks, `PlayerChatEvent` for mute checks
- Server switching via Velocity's `Player.createConnectionRequest()`
- Recommended for any multi-server setup

---

## Discord

**Module:** `brennon-discord`

### Installation

1. Build: `./gradlew :brennon-discord:shadowJar`
2. Run as a standalone process or include alongside another module
3. Configure the `discord` section in `brennon.json`:
   ```json
   {
     "discord": {
       "enabled": true,
       "token": "your-bot-token",
       "guildId": "your-guild-id",
       "chatChannelId": "channel-id-for-chat",
       "staffChannelId": "channel-id-for-staff",
       "alertChannelId": "channel-id-for-alerts"
     }
   }
   ```

### Features

- **Chat sync**: Minecraft chat messages appear in Discord and vice versa
- **Punishment alerts**: Ban/mute/kick actions posted to the alerts channel
- **Staff chat**: Staff messages bridged to a dedicated Discord channel
- **Player count**: Bot status shows current online player count
- Uses JDA (Java Discord API)

---

## Standalone

**Module:** `brennon-standalone`

### Installation

1. Build: `./gradlew :brennon-standalone:shadowJar`
2. Run: `java -jar brennon-standalone-2.0.0-SNAPSHOT-all.jar [data-folder]`
3. Configure `data/brennon.json`

### Notes

- Runs without any Minecraft server — headless process
- Useful for background tasks, scripts, or running the API without a game server
- Connects to the same database and Redis as other modules
- No GUI system, no chat events, no game-specific features
- Platform type: `STANDALONE`

---

## Web Server (Dashboard)

**Module:** `brennon-webserver`

### Installation

1. Build the backend:
   ```bash
   ./gradlew :brennon-webserver:shadowJar
   ```

2. Build the frontend (requires Node.js):
   ```bash
   cd brennon-webserver/frontend
   npm install
   npm run build
   ```

3. Rebuild the shadow JAR to include the frontend:
   ```bash
   ./gradlew :brennon-webserver:shadowJar
   ```

4. Run:
   ```bash
   java -jar brennon-webserver/build/libs/brennon-webserver-2.0.0-SNAPSHOT-all.jar [data-folder]
   ```

5. Configure `data/webserver.json` (see [CONFIGURATION.md](CONFIGURATION.md))
6. Access the dashboard at `http://localhost:8080`

### Features

- 50+ REST API endpoints covering all core manager functionality
- JWT + API key dual authentication
- WebSocket real-time events (18 Redis channels bridged)
- Vue 3 dark-theme dashboard with 13 views
- Runs as a standalone JAR (22MB)

### Frontend Development

For hot-reload development:

```bash
cd brennon-webserver/frontend
npm run dev
```

The Vite dev server proxies API requests to `http://localhost:8080`.

---

## Build All Modules

```bash
./gradlew shadowJar
```

This builds shadow JARs for all 15 modules. Output JARs are in each module's `build/libs/` directory.

**Important:** Use `./gradlew.bat` on Windows (Gradle 8.5 wrapper). Do not use a system-installed Gradle, as it may be an incompatible version.
