package com.envarcade.brennon.api.event;

import java.util.function.Consumer;

public interface EventBus {

    <T extends BrennonEvent> void subscribe(Class<T> eventClass, Consumer<T> handler);

    <T extends BrennonEvent> T publish(T event);

    void unsubscribeAll(Class<? extends BrennonEvent> eventClass);
}
