package ru.astrainteractive.messagebridge.messenger.telegram.di

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramMessageController

class CoreTelegramModule(
    coreModule: CoreModule,
) : Logger by JUtiltLogger("MessageBridge-CoreTelegramModule") {

    val telegramClientFlow = coreModule.configKrate.cachedStateFlow
        .mapCached<PluginConfiguration, OkHttpTelegramClient>(
            scope = coreModule.scope,
            transform = { config, prev ->
                val tgConfig = config.tgConfig
                info { "#telegramClientFlow creating telegram client ${tgConfig.token}_token" }
                val clinet = OkHttpTelegramClient(tgConfig.token)
                info { "#telegramClientFlow telegram client created!" }
                clinet
            }
        )

    val telegramMessageController = TelegramMessageController(
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
        telegramClientFlow = telegramClientFlow
    )
    val lifecycle = Lifecycle.Lambda()
}
