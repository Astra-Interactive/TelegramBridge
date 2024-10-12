package ru.astrainteractive.discordbot.module.bridge.serializer

import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.MessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.OnlineListMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.PingMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.PongMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.RequestOnlineMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineMessageData

internal object SocketRouteFactory {
    fun toRoute(data: MessageData) = when (data) {
        is BotMessageReceivedMessageData -> SocketRoute.BOT_MESSAGE_RECEIVED
        is OnlineListMessageData -> SocketRoute.ONLINE_LIST
        PingMessageData -> SocketRoute.PING
        PongMessageData -> SocketRoute.PONG
        RequestOnlineMessageData -> SocketRoute.REQUEST_ONLINE_LIST
        is ServerEventMessageData -> SocketRoute.SERVER_EVENT
        is UpdateOnlineMessageData -> SocketRoute.UPDATE_ONLINE
    }
}
