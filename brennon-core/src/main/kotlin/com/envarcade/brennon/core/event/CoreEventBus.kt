package com.envarcade.brennon.core.event

import com.envarcade.brennon.api.event.BrennonEvent
import com.envarcade.brennon.api.event.EventBus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/**
 * Core implementation of the Brennon event bus.
 *
 * Provides a lightweight, thread-safe pub/sub system for
 * cross-module communication within a single JVM.
 */
class CoreEventBus : EventBus {

    private val listeners = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<Consumer<*>>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : BrennonEvent> subscribe(eventClass: Class<T>, handler: Consumer<T>) {
        listeners.computeIfAbsent(eventClass) { CopyOnWriteArrayList() }
            .add(handler as Consumer<*>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BrennonEvent> publish(event: T): T {
        val eventListeners = listeners[event::class.java] ?: return event

        for (listener in eventListeners) {
            try {
                (listener as Consumer<T>).accept(event)
            } catch (e: Exception) {
                println("[Brennon] Error dispatching event ${event::class.simpleName}: ${e.message}")
                e.printStackTrace()
            }

            if (event.isCancelled) break
        }

        return event
    }

    override fun unsubscribeAll(eventClass: Class<out BrennonEvent>) {
        listeners.remove(eventClass)
    }

    /**
     * Clears all registered listeners.
     */
    fun clear() {
        listeners.clear()
    }
}
