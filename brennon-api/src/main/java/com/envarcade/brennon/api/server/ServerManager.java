package com.envarcade.brennon.api.server;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ServerManager {

    Optional<ServerInfo> getServer(String name);

    Collection<ServerInfo> getServers();

    Collection<ServerInfo> getOnlineServers();

    CompletableFuture<Void> sendPlayer(UUID uuid, String serverName);

    String getCurrentServer();

    // Server groups

    Optional<ServerGroupInfo> getGroup(String groupId);

    Collection<ServerGroupInfo> getGroups();

    Collection<ServerInfo> getServersByGroup(String groupId);
}
