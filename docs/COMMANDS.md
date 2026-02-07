# Command Reference

All commands are registered through the core command system and work identically across all platforms.

## Punishment Commands

### /ban
| | |
|---|---|
| **Usage** | `/ban <player> [duration] <reason>` |
| **Permission** | `brennon.command.ban` |
| **Aliases** | `networkban`, `nban` |
| **Description** | Ban a player from the network. Duration examples: `1h`, `1d`, `7d`, `30d`, `perm`. Omitting duration or using `perm` creates a permanent ban. |

### /unban
| | |
|---|---|
| **Usage** | `/unban <player>` |
| **Permission** | `brennon.command.unban` |
| **Aliases** | `pardon` |
| **Description** | Unban a player. |

### /mute
| | |
|---|---|
| **Usage** | `/mute <player> [duration] <reason>` |
| **Permission** | `brennon.command.mute` |
| **Aliases** | `networkmute` |
| **Description** | Mute a player on the network. Same duration format as `/ban`. |

### /unmute
| | |
|---|---|
| **Usage** | `/unmute <player>` |
| **Permission** | `brennon.command.unmute` |
| **Description** | Unmute a player. |

### /kick
| | |
|---|---|
| **Usage** | `/kick <player> [reason]` |
| **Permission** | `brennon.command.kick` |
| **Description** | Kick a player from the network. Default reason: "Kicked by staff". |

### /warn
| | |
|---|---|
| **Usage** | `/warn <player> <reason>` |
| **Permission** | `brennon.command.warn` |
| **Description** | Warn a player. The target receives a warning message if online. |

### /history
| | |
|---|---|
| **Usage** | `/history <player> [--all-networks]` |
| **Permission** | `brennon.command.history` |
| **Aliases** | `punishmenthistory`, `ph` |
| **Description** | View a player's punishment history. Use `--all-networks` (requires `brennon.admin.crossnetwork`) to view across all networks. Shows up to 10 entries. |

---

## Rank Commands

### /rank
| | |
|---|---|
| **Usage** | `/rank <set\|add\|remove\|list\|info> ...` |
| **Permission** | `brennon.command.rank` |
| **Aliases** | `ranks`, `setrank` |
| **Description** | Manage player ranks. |

**Subcommands:**

| Subcommand | Usage | Description |
|------------|-------|-------------|
| `set` | `/rank set <player> <rank>` | Set a player's primary rank |
| `add` | `/rank add <player> <rank>` | Add a secondary rank |
| `remove` | `/rank remove <player> <rank>` | Remove a rank |
| `list` | `/rank list` | List all ranks sorted by weight |
| `info` | `/rank info <rank>` | View rank details (prefix, weight, permissions, inheritance) |

---

## Economy Commands

### /eco
| | |
|---|---|
| **Usage** | `/eco <give\|take\|set\|balance> <player> [amount]` |
| **Permission** | `brennon.command.eco` |
| **Aliases** | `economy`, `bal` |
| **Description** | Manage player economy. |

**Subcommands:**

| Subcommand | Usage | Description |
|------------|-------|-------------|
| `give` | `/eco give <player> <amount>` | Deposit money |
| `take` | `/eco take <player> <amount>` | Withdraw money |
| `set` | `/eco set <player> <amount>` | Set exact balance |
| `balance` | `/eco balance [player]` | Check balance (defaults to self) |

---

## Server Commands

### /server
| | |
|---|---|
| **Usage** | `/server [name]` |
| **Permission** | `brennon.command.server` |
| **Aliases** | `servers`, `hub`, `lobby` |
| **Description** | List online servers (grouped by server group with player counts) or send yourself to a server. |

### /serveradmin
| | |
|---|---|
| **Usage** | `/sa <add\|remove\|list\|info\|setgroup>` |
| **Permission** | `brennon.admin.server` |
| **Aliases** | `sa` |
| **Description** | Manage the dynamic server registry. |

**Subcommands:**

| Subcommand | Usage | Description |
|------------|-------|-------------|
| `add` | `/sa add <name> <group> <host:port>` | Register a server |
| `remove` | `/sa remove <name>` | Unregister a server |
| `list` | `/sa list [group]` | List registered servers (optionally filter by group) |
| `info` | `/sa info <name>` | Show server details |
| `setgroup` | `/sa setgroup <name> <group>` | Move server to a different group |

### /servergroup
| | |
|---|---|
| **Usage** | `/sg <create\|delete\|list\|info\|set>` |
| **Permission** | `brennon.admin.servergroup` |
| **Aliases** | `sg` |
| **Description** | Manage server groups. |

**Subcommands:**

| Subcommand | Usage | Description |
|------------|-------|-------------|
| `create` | `/sg create <id> [displayName]` | Create a group |
| `delete` | `/sg delete <id>` | Delete a group (must have no servers) |
| `list` | `/sg list` | List all groups |
| `info` | `/sg info <id>` | Show group details and servers |
| `set` | `/sg set <id> <property> <value>` | Set group property (`displayName`, `joinPriority`, `restricted`, `permission`, `isFallback`, `maxPlayers`) |

---

## Staff Commands

### /staffmode
| | |
|---|---|
| **Usage** | `/staffmode` |
| **Permission** | `brennon.command.staffmode` |
| **Aliases** | `sm`, `staff` |
| **Description** | Toggle staff mode on/off. |

### /vanish
| | |
|---|---|
| **Usage** | `/vanish` |
| **Permission** | `brennon.command.vanish` |
| **Aliases** | `v` |
| **Description** | Toggle vanish on/off. |

### /staffchat
| | |
|---|---|
| **Usage** | `/staffchat <message>` |
| **Permission** | `brennon.command.staffchat` |
| **Aliases** | `sc` |
| **Description** | Send a message to the cross-server staff chat. |

### /stafflist
| | |
|---|---|
| **Usage** | `/stafflist` |
| **Permission** | `brennon.command.stafflist` |
| **Aliases** | `sl`, `onlinestaff` |
| **Description** | List all online staff across the network with their server and vanish status. |

### /report
| | |
|---|---|
| **Usage** | `/report <player> <reason>` |
| **Permission** | `brennon.command.report` |
| **Description** | File a report against a player. Players only. |

### /reports
| | |
|---|---|
| **Usage** | `/reports [claim <id>\|resolve <id>\|dismiss <id>]` |
| **Permission** | `brennon.command.reports` |
| **Aliases** | `viewreports` |
| **Description** | View and manage open reports. Without arguments, lists open reports. |

---

## Chat Commands

### /channel
| | |
|---|---|
| **Usage** | `/channel [name]` |
| **Permission** | `brennon.command.channel` |
| **Aliases** | `ch` |
| **Description** | Switch chat channel. Without arguments, shows current channel and available channels. Accepts channel ID or shortcut. |

### /msg
| | |
|---|---|
| **Usage** | `/msg <player> <message>` |
| **Permission** | `brennon.command.msg` |
| **Aliases** | `message`, `tell`, `whisper`, `w` |
| **Description** | Send a cross-server private message. |

### /reply
| | |
|---|---|
| **Usage** | `/reply <message>` |
| **Permission** | `brennon.command.reply` |
| **Aliases** | `r` |
| **Description** | Reply to the last private message received. |

### /chatfilter
| | |
|---|---|
| **Usage** | `/chatfilter <enable\|disable\|list>` |
| **Permission** | `brennon.admin.chatfilter` |
| **Description** | Manage chat filters. |

---

## Ticket Commands

### /ticket
| | |
|---|---|
| **Usage** | `/ticket <create\|view\|reply\|assign\|close\|list>` |
| **Permission** | `brennon.command.ticket` |
| **Aliases** | `tickets`, `t` |
| **Description** | Manage support tickets. |

**Subcommands:**

| Subcommand | Usage | Description |
|------------|-------|-------------|
| `create` | `/ticket create <subject>` | Create a new ticket |
| `view` | `/ticket view <id>` | View ticket details and messages |
| `reply` | `/ticket reply <id> <message>` | Add a reply |
| `assign` | `/ticket assign <id> <player>` | Assign to staff (requires `brennon.staff.ticket.assign`) |
| `close` | `/ticket close <id>` | Close a ticket |
| `list` | `/ticket list [mine\|open\|assigned]` | List tickets (default: `mine` for players, `open` for staff) |

---

## Stats Commands

### /stats
| | |
|---|---|
| **Usage** | `/stats [player] [--network <id>]` |
| **Permission** | `brennon.command.stats` |
| **Description** | View player statistics. Without a player name, shows your own stats. Use `--network <id>` (requires `brennon.admin.crossnetwork`) to view stats from a specific network. |

### /leaderboard
| | |
|---|---|
| **Usage** | `/leaderboard <stat> [page]` |
| **Permission** | `brennon.command.leaderboard` |
| **Aliases** | `lb`, `top` |
| **Description** | View stat leaderboards. 10 entries per page with gold/silver/bronze coloring. |

**Available stats:** `playtime`, `sessions`, `kills`, `deaths`, `kdr`, `kill_streak`, `highest_kill_streak`, `money_earned`, `money_spent`, `blocks_placed`, `blocks_broken`, `messages_sent`, `times_banned`, `times_muted`, `times_kicked`, `times_warned`, `reports_filed`, `reports_received`, `tickets_created`, `tickets_resolved`

---

## Permission Summary

| Permission | Commands |
|-----------|----------|
| `brennon.command.ban` | /ban |
| `brennon.command.unban` | /unban |
| `brennon.command.mute` | /mute |
| `brennon.command.unmute` | /unmute |
| `brennon.command.kick` | /kick |
| `brennon.command.warn` | /warn |
| `brennon.command.history` | /history |
| `brennon.command.rank` | /rank |
| `brennon.command.eco` | /eco |
| `brennon.command.server` | /server |
| `brennon.admin.server` | /serveradmin |
| `brennon.admin.servergroup` | /servergroup |
| `brennon.command.staffmode` | /staffmode |
| `brennon.command.vanish` | /vanish |
| `brennon.command.staffchat` | /staffchat |
| `brennon.command.stafflist` | /stafflist |
| `brennon.command.report` | /report |
| `brennon.command.reports` | /reports |
| `brennon.command.channel` | /channel |
| `brennon.command.msg` | /msg |
| `brennon.command.reply` | /reply |
| `brennon.admin.chatfilter` | /chatfilter |
| `brennon.command.ticket` | /ticket |
| `brennon.command.stats` | /stats |
| `brennon.command.leaderboard` | /leaderboard |
| `brennon.admin.crossnetwork` | Cross-network history/stats flag |
| `brennon.staff.ticket` | Ticket staff reply flag |
| `brennon.staff.ticket.assign` | Ticket assign subcommand |
