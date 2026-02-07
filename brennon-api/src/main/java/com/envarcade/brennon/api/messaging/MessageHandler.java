package com.envarcade.brennon.api.messaging;

@FunctionalInterface
public interface MessageHandler {

    void onMessage(String channel, String message);
}
