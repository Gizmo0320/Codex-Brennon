package com.envarcade.brennon.api.economy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyManager {

    CompletableFuture<Double> getBalance(UUID uuid);

    CompletableFuture<Void> setBalance(UUID uuid, double amount);

    CompletableFuture<Double> deposit(UUID uuid, double amount);

    CompletableFuture<Double> withdraw(UUID uuid, double amount);

    CompletableFuture<Void> transfer(UUID from, UUID to, double amount);

    CompletableFuture<Boolean> has(UUID uuid, double amount);
}
