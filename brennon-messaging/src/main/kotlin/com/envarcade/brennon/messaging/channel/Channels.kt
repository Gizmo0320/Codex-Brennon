package com.envarcade.brennon.messaging.channel

object Channels {
    const val PLAYER_JOIN = "player:join"
    const val PLAYER_QUIT = "player:quit"
    const val PLAYER_SWITCH = "player:switch"
    const val PUNISHMENT_ISSUED = "punishment:issued"
    const val PUNISHMENT_REVOKED = "punishment:revoked"
    const val RANK_UPDATE = "rank:update"
    const val ECONOMY_UPDATE = "economy:update"
    const val SERVER_STATUS = "server:status"
    const val STAFF_CHAT = "staff:chat"
    const val STAFF_ALERT = "staff:alert"
    const val BROADCAST = "broadcast"
    const val COMMAND_SYNC = "command:sync"
    const val CHAT_MESSAGE = "chat:message"
    const val CHAT_PRIVATE = "chat:private"
    const val TICKET_CREATE = "ticket:create"
    const val TICKET_UPDATE = "ticket:update"
    const val TICKET_REPLY = "ticket:reply"
    const val STAT_UPDATE = "stat:update"
    const val SERVER_REGISTRY_UPDATE = "server:registry:update"
    const val SERVER_GROUP_UPDATE = "server:group:update"
    const val PLAYER_KICK = "player:kick"
    const val LUCKPERMS_EDITOR_REQUEST = "luckperms:editor:request"
    const val LUCKPERMS_EDITOR_RESPONSE = "luckperms:editor:response"

    /** Returns a network-scoped chat message channel, or the global one if networkId is null. */
    fun chatMessage(networkId: String?): String =
        if (networkId != null) "chat:$networkId:message" else CHAT_MESSAGE

    /** Returns a network-scoped chat private channel, or the global one if networkId is null. */
    fun chatPrivate(networkId: String?): String =
        if (networkId != null) "chat:$networkId:private" else CHAT_PRIVATE
}
