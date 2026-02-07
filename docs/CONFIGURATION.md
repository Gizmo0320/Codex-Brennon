# Configuration Reference

Brennon uses a single `brennon.json` file for all server/plugin configuration. The web dashboard uses a separate `webserver.json`.

## brennon.json

### Root

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `serverName` | String | `"unknown"` | Unique name for this server instance |
| `serverGroup` | String | `"default"` | Server group this instance belongs to |
| `serverHost` | String | `""` | Public hostname or IP of this server |
| `serverPort` | Int | `25565` | Port this server listens on |
| `network` | NetworkConfig | — | Network isolation settings |
| `database` | DatabaseConfig | — | Database connection settings |
| `redis` | RedisConfig | — | Redis connection settings |
| `modules` | ModulesConfig | — | Toggle features on/off |
| `serverRegistry` | ServerRegistryConfig | — | Dynamic server registry settings |
| `chat` | ChatConfig | — | Chat channels and filters |
| `discord` | DiscordConfig | — | Discord bot integration |
| `web` | WebConfig | — | Embedded web API settings |

### NetworkConfig

Controls network isolation for multi-network deployments sharing the same database.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `networkId` | String | `"default"` | Unique network identifier |
| `dataSharing` | DataSharingConfig | — | Cross-network data sharing settings |

### DatabaseConfig

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `driver` | DatabaseDriver | `MONGODB` | Database driver: `MONGODB`, `MYSQL`, `MARIADB`, `POSTGRESQL` |
| `host` | String | `"localhost"` | Database host |
| `port` | Int | `27017` | Database port (27017 for Mongo, 3306 for MySQL, 5432 for Postgres) |
| `database` | String | `"brennon"` | Database name |
| `username` | String | `""` | Database username |
| `password` | String | `""` | Database password |
| `poolSize` | Int | `10` | Connection pool size |
| `uri` | String | `""` | Full connection URI (overrides host/port/user/pass if set) |

### RedisConfig

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `host` | String | `"localhost"` | Redis host |
| `port` | Int | `6379` | Redis port |
| `password` | String | `""` | Redis password (empty for no auth) |
| `database` | Int | `0` | Redis database index |
| `poolSize` | Int | `8` | Connection pool size |
| `timeout` | Int | `3000` | Connection timeout in milliseconds |
| `channelPrefix` | String | `"brennon:"` | Prefix for all Redis pub/sub channels |

### ModulesConfig

Toggle individual systems on or off. Disabled modules are not initialized and their commands are not registered.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `economy` | Boolean | `true` | Economy system (balance, deposit, withdraw, transfer) |
| `punishments` | Boolean | `true` | Punishment system (ban, mute, kick, warn) |
| `ranks` | Boolean | `true` | Rank system (hierarchy, permissions, prefix) |
| `serverManager` | Boolean | `true` | Server manager (server list, player counts) |
| `staffTools` | Boolean | `true` | Staff tools (staff mode, vanish, reports) |
| `chat` | Boolean | `true` | Chat system (channels, private messages, filters) |
| `tickets` | Boolean | `true` | Ticket system (create, reply, assign, close) |
| `stats` | Boolean | `true` | Statistics tracking and leaderboards |
| `gui` | Boolean | `true` | GUI menu framework |

### ChatConfig

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `channels` | List\<ChatChannelData\> | 4 defaults | Chat channel definitions (see below) |
| `filters` | List\<ChatFilterData\> | `[]` | Chat filter rules |
| `defaultChannel` | String | `"global"` | Default channel ID for new players |
| `localRadius` | Int | `100` | Block radius for local chat |

#### ChatChannelData

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique channel identifier |
| `displayName` | String | Display name shown in messages |
| `format` | String | MiniMessage format string. Placeholders: `<rank_prefix>`, `<player>`, `<message>` |
| `permission` | String | Permission required to see messages (empty = everyone) |
| `sendPermission` | String | Permission required to send messages (empty = everyone) |
| `isCrossServer` | Boolean | Whether messages are broadcast across servers |
| `isDefault` | Boolean | Whether this is the default channel |
| `shortcut` | String | Short alias for `/channel` command |
| `radius` | Int | Block radius for local channels (-1 = unlimited) |

Default channels: `global`, `staff`, `trade`, `local`.

### DiscordConfig

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable Discord bot integration |
| `token` | String | `""` | Discord bot token |
| `guildId` | String | `""` | Discord guild (server) ID |
| `chatChannelId` | String | `""` | Channel ID for chat sync |
| `staffChannelId` | String | `""` | Channel ID for staff alerts |
| `alertChannelId` | String | `""` | Channel ID for punishment alerts |
| `syncChat` | Boolean | `true` | Sync in-game chat to Discord |
| `syncPunishments` | Boolean | `true` | Post punishment actions to Discord |
| `showPlayerCount` | Boolean | `true` | Show player count in bot status |

### ServerRegistryConfig

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `autoRegistration` | Boolean | `true` | Auto-register servers when they connect |
| `autoUnregister` | Boolean | `true` | Auto-unregister servers after timeout |
| `unregisterTimeoutMs` | Long | `60000` | Timeout in ms before unregistering offline servers |
| `fallbackGroup` | String | `"lobby"` | Default fallback group for kicked players |

### WebConfig

Embedded lightweight web API (for `brennon-web` module, not the full dashboard).

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable the embedded web API |
| `port` | Int | `8080` | HTTP port |
| `apiKey` | String | `"change-me"` | API key for authentication |
| `corsOrigins` | String | `"*"` | Allowed CORS origins |

---

## webserver.json

Configuration for the standalone web dashboard (`brennon-webserver` module).

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `port` | Int | `8080` | HTTP server port |
| `apiKey` | String | `"change-me"` | API key for programmatic access |
| `corsOrigins` | String | `"*"` | Allowed CORS origins |
| `jwtSecret` | String | `"change-me-to-a-random-secret"` | Secret key for JWT signing (HMAC256) |
| `jwtExpirationMinutes` | Long | `480` | JWT token expiration time (default 8 hours) |
| `wsEnabled` | Boolean | `true` | Enable WebSocket endpoint |
| `dashboardUsers` | List\<DashboardUser\> | 1 default | Dashboard login credentials |

### DashboardUser

| Field | Type | Description |
|-------|------|-------------|
| `username` | String | Login username |
| `password` | String | Login password |

Default user: `admin` / `admin`. Change this before deploying.

---

## Example Configs

### Single Server

```json
{
  "serverName": "survival",
  "serverGroup": "survival",
  "database": {
    "driver": "MONGODB",
    "host": "localhost",
    "port": 27017,
    "database": "brennon"
  },
  "redis": {
    "host": "localhost",
    "port": 6379
  }
}
```

### Multi-Server Network

```json
{
  "serverName": "lobby-1",
  "serverGroup": "lobby",
  "serverHost": "10.0.0.2",
  "serverPort": 25565,
  "database": {
    "driver": "POSTGRESQL",
    "host": "db.example.com",
    "port": 5432,
    "database": "brennon",
    "username": "brennon",
    "password": "secret"
  },
  "redis": {
    "host": "redis.example.com",
    "port": 6379,
    "password": "redis-secret"
  },
  "serverRegistry": {
    "autoRegistration": true,
    "fallbackGroup": "lobby"
  }
}
```

### Web Dashboard

```json
{
  "port": 8080,
  "apiKey": "my-secret-api-key-here",
  "jwtSecret": "a-long-random-string-for-jwt-signing",
  "jwtExpirationMinutes": 480,
  "wsEnabled": true,
  "dashboardUsers": [
    { "username": "admin", "password": "strong-password-here" }
  ]
}
```
