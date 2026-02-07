package com.envarcade.brennon.common.util

import java.util.UUID

object UUIDUtil {

    fun fromString(input: String): UUID {
        return if (input.contains("-")) {
            UUID.fromString(input)
        } else {
            UUID.fromString(
                input.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(),
                    "$1-$2-$3-$4-$5"
                )
            )
        }
    }

    fun toUndashed(uuid: UUID): String = uuid.toString().replace("-", "")

    fun generateId(): String = UUID.randomUUID().toString().substring(0, 8)
}
