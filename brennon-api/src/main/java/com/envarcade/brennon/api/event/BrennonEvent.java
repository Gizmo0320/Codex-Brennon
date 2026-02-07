package com.envarcade.brennon.api.event;

import java.time.Instant;

public abstract class BrennonEvent {

    private final Instant timestamp;
    private boolean cancelled;

    protected BrennonEvent() {
        this.timestamp = Instant.now();
        this.cancelled = false;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
