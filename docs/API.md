# Developer API Reference

Brennon provides a Java API that can be accessed from any platform module. All interfaces are in the `brennon-api` module.

## Accessing the API

```java
import com.envarcade.brennon.api.BrennonAPI;
import com.envarcade.brennon.api.BrennonProvider;

BrennonAPI api = BrennonProvider.get();
```

The `BrennonAPI` interface provides access to all managers:

```java
public interface BrennonAPI {
    PlayerManager getPlayerManager();
    RankManager getRankManager();
    EconomyManager getEconomyManager();
    PunishmentManager getPunishmentManager();
    ServerManager getServerManager();
    MessagingService getMessagingService();
    ModuleManager getModuleManager();
    ChatManager getChatManager();
    TicketManager getTicketManager();
    StatsManager getStatsManager();
    GuiManager getGuiManager();
    Platform getPlatform();
    String getNetworkId();
}
```

## API Conventions

- All async operations return `CompletableFuture<T>`
- Lookups that may not find a result return `Optional<T>`
- UUIDs are used for player identification
- The API is defined in Java for maximum compatibility; implementations are in Kotlin

---

## PlayerManager

Manage network players across all servers.

```java
public interface PlayerManager {
    CompletableFuture<Optional<NetworkPlayer>> getPlayer(UUID uuid);
    CompletableFuture<Optional<NetworkPlayer>> getPlayer(String name);
    NetworkPlayer getOnlinePlayer(UUID uuid);
    boolean isOnline(UUID uuid);
    int getOnlineCount();
}
```

`NetworkPlayer` provides: `getUniqueId()`, `getName()`, `getRanks()`, `getFirstJoin()`, `getLastSeen()`, `getCurrentServer()`, `isOnline()`, `sendMessage(Component)`.

---

## RankManager

CRUD operations for ranks and player rank assignments.

```java
public interface RankManager {
    Optional<Rank> getRank(String id);
    Collection<Rank> getRanks();
    Rank getDefaultRank();
    CompletableFuture<Void> setPlayerRank(UUID uuid, String rankId);
    CompletableFuture<Void> addPlayerRank(UUID uuid, String rankId);
    CompletableFuture<Void> removePlayerRank(UUID uuid, String rankId);
}
```

`Rank` provides: `getId()`, `getDisplayName()`, `getPrefix()` (Adventure Component), `getWeight()`, `getPermissions()`, `getInheritance()`, `isDefault()`, `isStaff()`.

---

## EconomyManager

Player balance management.

```java
public interface EconomyManager {
    CompletableFuture<Double> getBalance(UUID uuid);
    CompletableFuture<Void> setBalance(UUID uuid, double amount);
    CompletableFuture<Double> deposit(UUID uuid, double amount);
    CompletableFuture<Double> withdraw(UUID uuid, double amount);
    CompletableFuture<Void> transfer(UUID from, UUID to, double amount);
    CompletableFuture<Boolean> has(UUID uuid, double amount);
}
```

---

## PunishmentManager

Issue and manage punishments.

```java
public interface PunishmentManager {
    CompletableFuture<Punishment> ban(UUID uuid, String reason, Duration duration, UUID issuer);
    CompletableFuture<Punishment> mute(UUID uuid, String reason, Duration duration, UUID issuer);
    CompletableFuture<Punishment> kick(UUID uuid, String reason, UUID issuer);
    CompletableFuture<Punishment> warn(UUID uuid, String reason, UUID issuer);
    CompletableFuture<Void> unban(UUID uuid, UUID issuer);
    CompletableFuture<Void> unmute(UUID uuid, UUID issuer);
    CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid);
    CompletableFuture<List<Punishment>> getHistory(UUID uuid);
    CompletableFuture<Boolean> isBanned(UUID uuid);
    CompletableFuture<Boolean> isMuted(UUID uuid);
}
```

`Punishment` provides: `getId()`, `getType()`, `getTarget()`, `getIssuer()`, `getReason()`, `getIssuedAt()`, `getExpiresAt()`, `isPermanent()`, `isActive()`, `getRevokedBy()`.

`PunishmentType` enum: `BAN`, `MUTE`, `KICK`, `WARN`.

Pass `null` for `duration` to create a permanent ban/mute.

---

## ServerManager

Server listing, groups, and player routing.

```java
public interface ServerManager {
    Optional<ServerInfo> getServer(String name);
    Collection<ServerInfo> getServers();
    Collection<ServerInfo> getOnlineServers();
    CompletableFuture<Void> sendPlayer(UUID uuid, String serverName);
    String getCurrentServer();
    Optional<ServerGroupInfo> getGroup(String groupId);
    Collection<ServerGroupInfo> getGroups();
    Collection<ServerInfo> getServersByGroup(String groupId);
}
```

`ServerInfo` provides: `getName()`, `getGroup()`, `getPlayerCount()`, `getMaxPlayers()`, `isOnline()`.

---

## ChatManager

Cross-server chat channels and private messaging.

```java
public interface ChatManager {
    Optional<ChatChannel> getChannel(String id);
    Collection<ChatChannel> getChannels();
    ChatChannel getDefaultChannel();
    ChatChannel getPlayerChannel(UUID player);
    void setPlayerChannel(UUID player, String channelId);
    void sendMessage(UUID sender, String senderName, String channelId, String message);
    void sendPrivateMessage(UUID sender, String senderName, UUID recipient, String message);
    boolean isMutedInChannel(UUID player, String channelId);
    void toggleChannelSubscription(UUID player, String channelId);
}
```

---

## TicketManager

Support ticket system.

```java
public interface TicketManager {
    CompletableFuture<Ticket> createTicket(UUID creator, String creatorName,
                                           String subject, String message, String server);
    CompletableFuture<Optional<Ticket>> getTicket(String id);
    CompletableFuture<List<Ticket>> getOpenTickets();
    CompletableFuture<List<Ticket>> getPlayerTickets(UUID player);
    CompletableFuture<List<Ticket>> getAssignedTickets(UUID staff);
    CompletableFuture<Void> addReply(String ticketId, UUID author, String authorName,
                                     String message, boolean isStaff);
    CompletableFuture<Void> assignTicket(String ticketId, UUID staff);
    CompletableFuture<Void> setStatus(String ticketId, TicketStatus status);
    CompletableFuture<Void> setPriority(String ticketId, TicketPriority priority);
    CompletableFuture<Void> closeTicket(String ticketId, UUID closedBy);
}
```

`TicketStatus` enum: `OPEN`, `IN_PROGRESS`, `CLOSED`, `RESOLVED`.
`TicketPriority` enum: `LOW`, `NORMAL`, `HIGH`, `URGENT`.

---

## StatsManager

Player statistics and leaderboards.

```java
public interface StatsManager {
    CompletableFuture<Double> getStat(UUID player, String statId);
    CompletableFuture<Map<String, Double>> getAllStats(UUID player);
    CompletableFuture<Void> incrementStat(UUID player, String statId, double amount);
    CompletableFuture<Void> setStat(UUID player, String statId, double value);
    CompletableFuture<Map<UUID, Double>> getLeaderboard(String statId, int limit);
    CompletableFuture<Integer> getLeaderboardPosition(UUID player, String statId);
    CompletableFuture<Void> resetStat(UUID player, String statId);
    CompletableFuture<Void> resetAllStats(UUID player);
}
```

### Built-in Stat Types

| Constant | ID | Category |
|----------|----|----------|
| `StatTypes.PLAYTIME` | `playtime` | Time |
| `StatTypes.SESSIONS` | `sessions` | Time |
| `StatTypes.KILLS` | `kills` | PvP |
| `StatTypes.DEATHS` | `deaths` | PvP |
| `StatTypes.KDR` | `kdr` | PvP |
| `StatTypes.KILL_STREAK` | `kill_streak` | PvP |
| `StatTypes.HIGHEST_KILL_STREAK` | `highest_kill_streak` | PvP |
| `StatTypes.MONEY_EARNED` | `money_earned` | Economy |
| `StatTypes.MONEY_SPENT` | `money_spent` | Economy |
| `StatTypes.BLOCKS_PLACED` | `blocks_placed` | Blocks |
| `StatTypes.BLOCKS_BROKEN` | `blocks_broken` | Blocks |
| `StatTypes.MESSAGES_SENT` | `messages_sent` | Chat |
| `StatTypes.TIMES_BANNED` | `times_banned` | Punishments |
| `StatTypes.TIMES_MUTED` | `times_muted` | Punishments |
| `StatTypes.TIMES_KICKED` | `times_kicked` | Punishments |
| `StatTypes.TIMES_WARNED` | `times_warned` | Punishments |
| `StatTypes.REPORTS_FILED` | `reports_filed` | Reports |
| `StatTypes.REPORTS_RECEIVED` | `reports_received` | Reports |
| `StatTypes.TICKETS_CREATED` | `tickets_created` | Tickets |
| `StatTypes.TICKETS_RESOLVED` | `tickets_resolved` | Tickets |

Custom stat IDs can be used — they are stored alongside built-in stats.

---

## GuiManager

Cross-platform GUI (inventory menu) framework.

```java
public interface GuiManager {
    GuiBuilder createGui(String title, int rows);
    void openGui(UUID player, BrennonGui gui);
    void closeGui(UUID player);
    boolean hasOpenGui(UUID player);
}
```

Each platform module translates `BrennonGui` into native inventory implementations.

---

## EventBus

Subscribe to and publish internal events.

```java
public interface EventBus {
    <T extends BrennonEvent> void subscribe(Class<T> eventClass, Consumer<T> handler);
    <T extends BrennonEvent> T publish(T event);
    void unsubscribeAll(Class<? extends BrennonEvent> eventClass);
}
```

### Example

```java
BrennonAPI api = BrennonProvider.get();
api.getEventBus().subscribe(PlayerNetworkJoinEvent.class, event -> {
    System.out.println(event.getName() + " joined on " + event.getServer());
});
```

---

## MessagingService

Low-level Redis pub/sub access.

```java
public interface MessagingService {
    void publish(String channel, String message);
    void subscribe(String channel, MessageHandler handler);
    void unsubscribe(String channel);
    boolean isConnected();
}
```

---

## ModuleManager

Register and manage modules.

```java
public interface ModuleManager {
    void registerModule(Module module);
    Optional<Module> getModule(String id);
    Collection<Module> getModules();
    void enableModule(String id);
    void disableModule(String id);
    Collection<Module> getEnabledModules();
}
```

---

## Event Reference

All events extend `BrennonEvent`.

### Player Events

| Event | Fields |
|-------|--------|
| `PlayerNetworkJoinEvent` | `uuid: UUID`, `name: String`, `server: String` |
| `PlayerNetworkQuitEvent` | `uuid: UUID`, `name: String`, `lastServer: String` |
| `PlayerServerSwitchEvent` | `uuid: UUID`, `name: String`, `fromServer: String`, `toServer: String` |

### Rank Events

| Event | Fields |
|-------|--------|
| `PlayerRankChangeEvent` | `uuid: UUID`, `oldRank: String`, `newRank: String`, `changedBy: UUID?` |

### Economy Events

| Event | Fields |
|-------|--------|
| `BalanceChangeEvent` | `uuid: UUID`, `oldBalance: Double`, `newBalance: Double`, `reason: String` |
| `TransferEvent` | `from: UUID`, `to: UUID`, `amount: Double` |

### Punishment Events

| Event | Fields |
|-------|--------|
| `PunishmentIssuedEvent` | `punishmentId: String`, `target: UUID`, `issuer: UUID?`, `type: PunishmentType`, `reason: String` |
| `PunishmentRevokedEvent` | `punishmentId: String`, `target: UUID`, `revokedBy: UUID?`, `type: PunishmentType` |

### Chat Events

| Event | Fields |
|-------|--------|
| `ChatMessageEvent` | `sender: UUID`, `senderName: String`, `channelId: String`, `message: String`, `server: String` |
| `PrivateMessageEvent` | `sender: UUID`, `senderName: String`, `recipient: UUID`, `message: String` |
| `ChannelSwitchEvent` | `player: UUID`, `oldChannel: String`, `newChannel: String` |

### Ticket Events

| Event | Fields |
|-------|--------|
| `TicketCreateEvent` | `ticketId: String`, `creator: UUID`, `creatorName: String`, `subject: String` |
| `TicketReplyEvent` | `ticketId: String`, `author: UUID`, `authorName: String`, `isStaff: Boolean` |
| `TicketStatusChangeEvent` | `ticketId: String`, `oldStatus: TicketStatus`, `newStatus: TicketStatus`, `changedBy: UUID?` |
| `TicketAssignEvent` | `ticketId: String`, `assignee: UUID`, `assignedBy: UUID?` |

### Stats Events

| Event | Fields |
|-------|--------|
| `StatChangeEvent` | `player: UUID`, `statId: String`, `oldValue: Double`, `newValue: Double` |

### Server Registry Events

| Event | Fields |
|-------|--------|
| `ServerRegisteredEvent` | `serverName: String`, `group: String`, `host: String`, `port: Int`, `autoRegistered: Boolean` |
| `ServerUnregisteredEvent` | `serverName: String`, `group: String` |
| `ServerGroupCreatedEvent` | `groupId: String`, `displayName: String` |
| `ServerGroupDeletedEvent` | `groupId: String` |

---

## Code Examples

### Ban a player for 7 days

```java
BrennonAPI api = BrennonProvider.get();
UUID target = /* player UUID */;
UUID issuer = /* staff UUID */;

api.getPunishmentManager()
    .ban(target, "Griefing", Duration.ofDays(7), issuer)
    .thenAccept(punishment -> {
        System.out.println("Banned: " + punishment.getId());
    });
```

### Check a player's balance

```java
api.getEconomyManager().getBalance(playerUuid).thenAccept(balance -> {
    System.out.println("Balance: $" + balance);
});
```

### Listen for player joins

```java
api.getEventBus().subscribe(PlayerNetworkJoinEvent.class, event -> {
    api.getEconomyManager().deposit(event.getUuid(), 100.0);
});
```

### Get the kills leaderboard

```java
api.getStatsManager().getLeaderboard("kills", 10).thenAccept(leaderboard -> {
    leaderboard.forEach((uuid, kills) -> {
        System.out.println(uuid + ": " + kills + " kills");
    });
});
```
