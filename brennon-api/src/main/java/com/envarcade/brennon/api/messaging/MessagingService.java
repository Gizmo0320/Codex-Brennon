package com.envarcade.brennon.api.messaging;

public interface MessagingService {

    void publish(String channel, String message);

    void subscribe(String channel, MessageHandler handler);

    void unsubscribe(String channel);

    boolean isConnected();
}
