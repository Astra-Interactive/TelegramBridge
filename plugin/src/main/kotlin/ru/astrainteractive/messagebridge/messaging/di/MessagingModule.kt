package ru.astrainteractive.messagebridge.messaging.di

import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.di.TelegramModule
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.TelegramMessageController

class MessagingModule(
    coreModule: CoreModule,
    telegramModule: TelegramModule
) {
    val minecraftMessageController = MinecraftMessageController(
        kyoriKrate = coreModule.kyoriKrate,
        translationKrate = coreModule.translationKrate
    )

    val telegramMessageController = TelegramMessageController(
        configKrate = coreModule.configKrate,
        telegramClientFlow = telegramModule.telegramClientFlow,
        translationKrate = coreModule.translationKrate
    )
}
