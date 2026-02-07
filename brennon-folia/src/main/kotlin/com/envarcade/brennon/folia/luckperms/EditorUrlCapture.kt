package com.envarcade.brennon.folia.luckperms

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.command.ConsoleCommandSender
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture

/**
 * Creates a proxy around ConsoleCommandSender that intercepts sendMessage(Component)
 * calls to capture the LuckPerms web editor URL from ClickEvent.OPEN_URL.
 *
 * Uses Java dynamic proxy because Kotlin's `by` delegation doesn't allow
 * overriding Java default interface methods (sendMessage is a default on Audience).
 */
object EditorUrlCapture {

    fun create(delegate: ConsoleCommandSender): Pair<ConsoleCommandSender, CompletableFuture<String>> {
        val urlFuture = CompletableFuture<String>()

        val handler = InvocationHandler { _, method, args ->
            // Intercept sendMessage calls to look for LP editor URLs
            if (method.name == "sendMessage" && args != null && args.size == 1 && args[0] is Component) {
                val message = args[0] as Component
                extractUrl(message)?.let { url ->
                    if (!urlFuture.isDone) urlFuture.complete(url)
                }
            }

            // Delegate to the real console sender
            if (args == null) {
                method.invoke(delegate)
            } else {
                method.invoke(delegate, *args)
            }
        }

        val proxy = Proxy.newProxyInstance(
            delegate.javaClass.classLoader,
            delegate.javaClass.interfaces,
            handler
        ) as ConsoleCommandSender

        return proxy to urlFuture
    }

    private fun extractUrl(component: Component): String? {
        val click = component.clickEvent()
        if (click != null && click.action() == ClickEvent.Action.OPEN_URL) {
            val url = click.value()
            if (url.contains("luckperms.net/editor")) return url
        }
        for (child in component.children()) {
            extractUrl(child)?.let { return it }
        }
        return null
    }
}
