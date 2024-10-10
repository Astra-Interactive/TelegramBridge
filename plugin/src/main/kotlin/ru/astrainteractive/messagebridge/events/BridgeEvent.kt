package ru.astrainteractive.messagebridge.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.messagebridge.messaging.MessageController

/**
 * Events from discord by socket
 */
class BridgeEvent(
    clientBridgeApi: BridgeApi,
    minecraftMessageController: MessageController,
    telegramMessageController: MessageController
) : CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO) {

    init {
        clientBridgeApi
            .eventFlow()
            .onEach {
                minecraftMessageController.send(it)
                telegramMessageController.send(it)
            }.launchIn(this)
    }
}
