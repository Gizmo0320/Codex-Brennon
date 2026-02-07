package com.envarcade.brennon.api.punishment;

import java.time.Instant;
import java.util.UUID;

public interface Punishment {

    String getId();

    UUID getTarget();

    UUID getIssuer();

    PunishmentType getType();

    String getReason();

    Instant getIssuedAt();

    Instant getExpiresAt();

    boolean isActive();

    boolean isPermanent();

    boolean isRevoked();

    UUID getRevokedBy();
}
