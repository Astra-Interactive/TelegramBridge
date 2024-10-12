package ru.astrainteractive.messagebridge.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.MessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.OnlineListMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.PingMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.PongMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.RequestOnlineMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineMessageData
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.TelegramMessageController

/**
 * Events from discord by socket
 */
class BridgeEvent(
    clientBridgeApi: BridgeApi,
    private val minecraftMessageController: MinecraftMessageController,
    private val telegramMessageController: TelegramMessageController
) : CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO),
    Logger by JUtiltLogger("MessageBridge-BridgeEvent") {

    private suspend fun onEvent(data: MessageData) {
        when (data) {
            RequestOnlineMessageData,
            PongMessageData,
            PingMessageData,
            is UpdateOnlineMessageData,
            is BotMessageReceivedMessageData,
            is OnlineListMessageData -> Unit

            is ServerEventMessageData -> {
                minecraftMessageController.send(data.instance)
                telegramMessageController.send(data.instance)
            }
        }
    }

    init {
        clientBridgeApi
            .eventFlow()
            .onEach { onEvent(it) }
            .launchIn(this)
    }
}
