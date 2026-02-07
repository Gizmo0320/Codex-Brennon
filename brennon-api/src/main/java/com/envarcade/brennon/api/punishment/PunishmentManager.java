package com.envarcade.brennon.api.punishment;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
