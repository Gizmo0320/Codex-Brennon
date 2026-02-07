package com.envarcade.brennon.common.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

object TextUtil {

    private val miniMessage = MiniMessage.miniMessage()

    fun parse(text: String): Component = miniMessage.deserialize(text)

    fun parse(text: String, vararg replacements: Pair<String, String>): Component {
        var processed = text
        for ((key, value) in replacements) {
            processed = processed.replace("<$key>", value)
        }
        return miniMessage.deserialize(processed)
    }

    fun serialize(component: Component): String = miniMessage.serialize(component)

    fun stripFormatting(text: String): String = miniMessage.stripTags(text)

    fun prefixed(message: String): Component =
        parse("<gradient:#6C63FF:#4ECDC4><bold>Brennon</bold></gradient> <dark_gray>» <gray>$message")

    fun error(message: String): Component =
        parse("<gradient:#6C63FF:#4ECDC4><bold>Brennon</bold></gradient> <dark_gray>» <red>$message")

    fun success(message: String): Component =
        parse("<gradient:#6C63FF:#4ECDC4><bold>Brennon</bold></gradient> <dark_gray>» <green>$message")
}
