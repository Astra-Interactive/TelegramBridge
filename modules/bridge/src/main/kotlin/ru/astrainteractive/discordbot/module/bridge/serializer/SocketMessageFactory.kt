package ru.astrainteractive.discordbot.module.bridge.serializer

import ru.astrainteractive.discordbot.module.bridge.model.SocketBotMessageReceivedMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketOnlineListMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPingMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRequestOnlineListMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.SocketServerEventMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketUpdateOnlineMessage
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.OnlineListMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineMessageData

internal object SocketMessageFactory {
    fun <T> create(
        route: SocketRoute,
        data: T? = null,
        getId: () -> Long
    ): SocketMessage {
        return when (route) {
            SocketRoute.PING -> SocketPingMessage(id = getId.invoke())

            SocketRoute.PONG -> SocketPongMessage(id = getId.invoke())

            SocketRoute.MESSAGE -> SocketServerEventMessage(
                id = getId.invoke(),
                data = data as ServerEventMessageData
            )

            SocketRoute.UPDATE_ONLINE -> SocketUpdateOnlineMessage(
                id = getId.invoke(),
                data = data as UpdateOnlineMessageData
            )

            SocketRoute.BOT_MESSAGE_RECEIVED -> SocketBotMessageReceivedMessage(
                id = getId.invoke(),
                data = data as BotMessageReceivedMessageData
            )

            SocketRoute.REQUEST_ONLINE_LIST -> SocketRequestOnlineListMessage(id = getId.invoke())

            SocketRoute.ONLINE_LIST -> SocketOnlineListMessage(
                id = getId.invoke(),
                data = data as OnlineListMessageData
            )
        }
    }
}
