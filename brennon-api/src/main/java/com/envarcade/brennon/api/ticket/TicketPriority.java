package com.envarcade.brennon.api.ticket;

public enum TicketPriority {
    LOW(0),
    NORMAL(1),
    HIGH(2),
    URGENT(3);

    private final int weight;

    TicketPriority(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
