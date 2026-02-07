# REST API Reference

The Brennon Web Server provides a full REST API for managing all network systems. It runs as a standalone process alongside your Minecraft network.

## Base URL

```
http://localhost:8080
```

Port is configurable in `webserver.json`.

## Authentication

All `/api/*` endpoints (except `/api/auth/login`) require authentication via the `Authorization` header.

Two authentication methods are supported:

### JWT Token (Dashboard Users)

1. Call `POST /api/auth/login` with username/password
2. Use the returned token: `Authorization: Bearer <jwt-token>`

### API Key (Programmatic Access)

Use the API key from `webserver.json`: `Authorization: Bearer <api-key>`

---

## Auth Endpoints

### POST /api/auth/login

Login and obtain a JWT token. **No authentication required.**

**Request:**
```json
{ "username": "admin", "password": "admin" }
```

**Response:**
```json
{ "token": "eyJ...", "username": "admin" }
```

### POST /api/auth/refresh

Refresh an existing JWT token.

**Response:**
```json
{ "token": "eyJ..." }
```

### GET /api/auth/me

Get the current authenticated user.

**Response:**
```json
{ "username": "admin", "role": "admin" }
```

---

## Module Endpoints

### GET /api/modules

Get the enabled/disabled status of all modules.

**Response:**
```json
{
  "economy": true,
  "punishments": true,
  "ranks": true,
  "serverManager": true,
  "staffTools": true,
  "chat": true,
  "tickets": true,
  "stats": true,
  "gui": true
}
```

---

## Player Endpoints

### GET /api/players/:uuid

Get a player by UUID.

**Response:**
```json
{
  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "name": "Notch",
  "ranks": ["admin"],
  "firstJoin": "2024-01-15T10:30:00Z",
  "lastSeen": "2024-06-01T18:45:00Z",
  "lastServer": "lobby-1",
  "online": true
}
```

### GET /api/players/name/:name

Get a player by name.

### GET /api/players/online

Get all online players.

**Response:**
```json
[
  { "uuid": "...", "name": "Player1", "server": "survival-1" },
  { "uuid": "...", "name": "Player2", "server": "lobby-1" }
]
```

### GET /api/players/network/sessions

Get all active network sessions (UUID to server mapping).

### GET /api/players/:uuid/server

Get which server a player is currently on.

**Response:**
```json
{ "uuid": "...", "server": "survival-1" }
```

### GET /api/players/network/count

Get total online player count.

**Response:**
```json
{ "count": 42 }
```

---

## Rank Endpoints

### GET /api/ranks

Get all ranks.

**Response:**
```json
[
  {
    "id": "admin",
    "displayName": "Admin",
    "prefix": "<red>[Admin]</red>",
    "weight": 100,
    "permissions": ["*"],
    "isDefault": false,
    "isStaff": true
  }
]
```

### GET /api/ranks/:id

Get a rank by ID.

### POST /api/ranks

Create a new rank.

**Request:**
```json
{
  "id": "vip",
  "displayName": "VIP",
  "prefix": "<gold>[VIP]</gold>",
  "weight": 50,
  "permissions": ["brennon.command.server"],
  "isDefault": false,
  "isStaff": false
}
```

### PUT /api/ranks/:id

Update an existing rank. Same body as create.

### DELETE /api/ranks/:id

Delete a rank.

### POST /api/ranks/set/:uuid/:rankId

Set a player's primary rank.

### POST /api/ranks/add/:uuid/:rankId

Add a secondary rank to a player.

### DELETE /api/ranks/remove/:uuid/:rankId

Remove a rank from a player.

### POST /api/ranks/reload

Reload all ranks from the database.

---

## Punishment Endpoints

### GET /api/punishments/:uuid

Get full punishment history for a player.

**Response:**
```json
[
  {
    "id": "abc123",
    "type": "BAN",
    "reason": "Griefing",
    "issuedBy": "069a79f4-...",
    "issuedAt": "2024-06-01T12:00:00Z",
    "expiresAt": "2024-06-08T12:00:00Z",
    "isPermanent": false,
    "isActive": true,
    "revokedBy": null
  }
]
```

### GET /api/punishments/:uuid/active

Get only active punishments for a player.

### POST /api/punishments/ban

Ban a player.

**Request:**
```json
{
  "target": "player-uuid",
  "reason": "Griefing",
  "issuer": "staff-uuid",
  "duration": 604800000
}
```

`duration` is in milliseconds. Use `0` for permanent.

### POST /api/punishments/mute

Mute a player. Same request format as ban.

### POST /api/punishments/kick

Kick a player.

**Request:**
```json
{
  "target": "player-uuid",
  "reason": "AFK",
  "issuer": "staff-uuid"
}
```

### POST /api/punishments/warn

Warn a player. Same request format as kick.

### DELETE /api/punishments/unban/:uuid

Unban a player.

### DELETE /api/punishments/unmute/:uuid

Unmute a player.

---

## Economy Endpoints

### GET /api/economy/:uuid

Get a player's balance.

**Response:**
```json
{ "uuid": "...", "balance": 1500.50 }
```

### POST /api/economy/deposit

Deposit money.

**Request:**
```json
{ "uuid": "player-uuid", "amount": 500.0 }
```

**Response:**
```json
{ "success": true, "balance": 2000.50 }
```

### POST /api/economy/withdraw

Withdraw money. Same request format.

### POST /api/economy/set

Set exact balance. Same request format.

### POST /api/economy/transfer

Transfer between players.

**Request:**
```json
{ "from": "uuid-1", "to": "uuid-2", "amount": 100.0 }
```

### GET /api/economy/:uuid/has/:amount

Check if a player has at least the specified amount.

**Response:**
```json
{ "uuid": "...", "amount": 500.0, "has": true }
```

---

## Server Endpoints

### GET /api/servers

Get all servers.

**Response:**
```json
[
  {
    "name": "lobby-1",
    "group": "lobby",
    "playerCount": 15,
    "maxPlayers": 100,
    "online": true
  }
]
```

### GET /api/servers/online

Get online server summary with total player count.

**Response:**
```json
{
  "totalPlayers": 142,
  "serverCount": 5,
  "servers": [
    { "name": "lobby-1", "playerCount": 15 }
  ]
}
```

### GET /api/servers/:name

Get a specific server by name.

### POST /api/servers/send

Send a player to a server.

**Request:**
```json
{ "uuid": "player-uuid", "server": "survival-1" }
```

### GET /api/servers/groups

Get all server groups.

**Response:**
```json
[
  { "id": "lobby", "displayName": "Lobby", "serverCount": 2 }
]
```

### GET /api/servers/groups/:id

Get a group with its servers.

---

## Ticket Endpoints

*Requires `tickets` module enabled.*

### GET /api/tickets

Get all open tickets.

### GET /api/tickets/:id

Get a ticket with all messages.

**Response:**
```json
{
  "id": "TICK-001",
  "subject": "Griefing report",
  "status": "OPEN",
  "priority": "NORMAL",
  "creatorName": "Player1",
  "server": "survival-1",
  "assignee": null,
  "messages": [
    {
      "authorName": "Player1",
      "content": "Someone broke my house",
      "timestamp": "2024-06-01T12:00:00Z",
      "isStaffMessage": false
    }
  ]
}
```

### GET /api/tickets/player/:uuid

Get all tickets created by a player.

### GET /api/tickets/assigned/:uuid

Get tickets assigned to a staff member.

### POST /api/tickets

Create a new ticket.

**Request:**
```json
{
  "creator": "player-uuid",
  "creatorName": "Player1",
  "subject": "Griefing report",
  "description": "Someone broke my house on survival-1",
  "server": "survival-1"
}
```

### POST /api/tickets/:id/reply

Add a reply to a ticket.

**Request:**
```json
{
  "author": "staff-uuid",
  "authorName": "Admin",
  "content": "We'll investigate this.",
  "isStaff": true
}
```

### PUT /api/tickets/:id/assign

Assign a ticket.

**Request:**
```json
{ "assignee": "staff-uuid" }
```

### PUT /api/tickets/:id/status

Change ticket status.

**Request:**
```json
{ "status": "IN_PROGRESS" }
```

Values: `OPEN`, `IN_PROGRESS`, `CLOSED`, `RESOLVED`.

### PUT /api/tickets/:id/priority

Change ticket priority.

**Request:**
```json
{ "priority": "HIGH" }
```

Values: `LOW`, `NORMAL`, `HIGH`, `URGENT`.

### PUT /api/tickets/:id/close

Close a ticket.

**Request:**
```json
{ "closedBy": "staff-uuid" }
```

---

## Stats Endpoints

*Requires `stats` module enabled.*

### GET /api/stats/:uuid

Get all stats for a player.

**Response:**
```json
{
  "uuid": "...",
  "stats": {
    "kills": 150.0,
    "deaths": 45.0,
    "playtime": 360000.0
  }
}
```

### GET /api/stats/:uuid/:statId

Get a specific stat value.

**Response:**
```json
{ "uuid": "...", "statId": "kills", "value": 150.0 }
```

### GET /api/stats/:uuid/:statId/position

Get a player's leaderboard position for a stat.

**Response:**
```json
{ "uuid": "...", "statId": "kills", "position": 3 }
```

### GET /api/leaderboard/:statId

Get the leaderboard for a stat. Query param: `?limit=10`.

**Response:**
```json
{
  "statId": "kills",
  "entries": [
    { "rank": 1, "uuid": "...", "name": "TopKiller", "value": 500.0 },
    { "rank": 2, "uuid": "...", "name": "Player2", "value": 320.0 }
  ]
}
```

### POST /api/stats/increment

Increment a stat.

**Request:**
```json
{ "uuid": "player-uuid", "statId": "kills", "amount": 1.0 }
```

### POST /api/stats/set

Set a stat to an exact value.

**Request:**
```json
{ "uuid": "player-uuid", "statId": "kills", "value": 100.0 }
```

### DELETE /api/stats/:uuid/:statId

Reset a specific stat.

### DELETE /api/stats/:uuid

Reset all stats for a player.

---

## Chat Endpoints

*Requires `chat` module enabled.*

### GET /api/chat/channels

Get all chat channels.

**Response:**
```json
[
  { "id": "global", "displayName": "Global", "isCrossServer": true, "isDefault": true },
  { "id": "staff", "displayName": "Staff", "isCrossServer": true, "isDefault": false }
]
```

### GET /api/chat/channels/:id

Get a specific channel.

### GET /api/chat/:uuid/channel

Get which channel a player is focused on.

**Response:**
```json
{ "uuid": "...", "channel": "global", "displayName": "Global" }
```

### PUT /api/chat/:uuid/channel

Set a player's focused channel.

**Request:**
```json
{ "channelId": "staff" }
```

### POST /api/chat/send

Send a chat message.

**Request:**
```json
{
  "sender": "player-uuid",
  "senderName": "Player1",
  "channelId": "global",
  "message": "Hello everyone!"
}
```

### POST /api/chat/dm

Send a private message.

**Request:**
```json
{
  "sender": "sender-uuid",
  "senderName": "Player1",
  "recipient": "recipient-uuid",
  "message": "Hey there!"
}
```

---

## Staff Endpoints

*Requires `staffTools` module enabled.*

### GET /api/staff

Get all online staff members.

**Response:**
```json
[
  {
    "uuid": "...",
    "name": "AdminPlayer",
    "server": "lobby-1",
    "vanished": false
  }
]
```

### GET /api/staff/:uuid

Get a specific staff member's status.

**Response:**
```json
{ "uuid": "...", "staffMode": true, "vanished": false }
```

### POST /api/staff/:uuid/toggle

Toggle staff mode.

**Request:**
```json
{ "name": "AdminPlayer" }
```

### POST /api/staff/:uuid/vanish

Toggle vanish.

---

## Report Endpoints

*Requires `staffTools` module enabled.*

### GET /api/reports

Get all open reports.

**Response:**
```json
[
  {
    "id": "rpt-abc123",
    "reporter": "reporter-uuid",
    "reporterName": "Player1",
    "target": "target-uuid",
    "targetName": "Griefer",
    "reason": "Breaking blocks in spawn",
    "server": "survival-1",
    "timestamp": 1717243200000,
    "status": "OPEN",
    "handledBy": null
  }
]
```

### POST /api/reports

Create a report.

**Request:**
```json
{
  "reporter": "reporter-uuid",
  "reporterName": "Player1",
  "target": "target-uuid",
  "targetName": "Griefer",
  "reason": "Breaking blocks in spawn"
}
```

### PUT /api/reports/:id/claim

Claim a report.

**Request:**
```json
{ "staffUuid": "staff-uuid" }
```

### PUT /api/reports/:id/resolve

Resolve or dismiss a report.

**Request:**
```json
{ "status": "RESOLVED" }
```

Values: `RESOLVED`, `DISMISSED`.

---

## WebSocket

Real-time events are available via WebSocket at `ws://localhost:8080/ws`.

### Connection

Connect with a JWT token as a query parameter:

```
ws://localhost:8080/ws?token=<jwt-token>
```

Connections without a valid token are rejected with close code `4001`.

### Client Messages

**Subscribe to events:**
```json
{
  "type": "subscribe",
  "data": { "events": ["player_join", "player_quit", "chat_message"] }
}
```

**Unsubscribe:**
```json
{
  "type": "unsubscribe",
  "data": { "events": ["chat_message"] }
}
```

**Ping:**
```json
{ "type": "ping" }
```

Clients with no subscriptions receive all events.

### Server Messages

**Event broadcast:**
```json
{
  "type": "player_join",
  "data": { "uuid": "...", "name": "Player1", "server": "lobby-1" },
  "timestamp": 1717243200000
}
```

**Subscription confirmation:**
```json
{
  "type": "subscribed",
  "data": { "events": ["player_join", "player_quit", "chat_message"] }
}
```

### Event Types

Events are bridged from Redis pub/sub channels to WebSocket:

| WebSocket Event | Redis Channel | Description |
|----------------|---------------|-------------|
| `player_join` | `player:join` | Player joined the network |
| `player_quit` | `player:quit` | Player left the network |
| `player_switch` | `player:switch` | Player switched servers |
| `chat_message` | `chat:message` | Chat message sent |
| `chat_private` | `chat:private` | Private message sent |
| `punishment_created` | `punishment:issued` | Punishment issued |
| `punishment_revoked` | `punishment:revoked` | Punishment revoked |
| `ticket_created` | `ticket:create` | Ticket created |
| `ticket_updated` | `ticket:update` | Ticket status changed |
| `ticket_reply` | `ticket:reply` | Ticket reply added |
| `economy_transaction` | `economy:update` | Economy balance changed |
| `server_status` | `server:status` | Server status update |
| `staff_alert` | `staff:alert` | Staff alert |
| `staff_chat` | `staff:chat` | Staff chat message |
| `rank_update` | `rank:update` | Rank changed |
| `stat_update` | `stat:update` | Stat changed |
| `broadcast` | `broadcast` | Network broadcast |
| `server_registry_update` | `server:registry:update` | Server registry changed |
