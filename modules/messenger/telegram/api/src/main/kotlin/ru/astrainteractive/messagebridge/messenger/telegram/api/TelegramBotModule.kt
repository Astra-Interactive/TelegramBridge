package ru.astrainteractive.messagebridge.messenger.telegram.api

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

interface TelegramBotModule {
    val state: StateFlow<TelegramConnectionState>

    val deliveryError: StateFlow<LocalizableComponent?>

    val botApi: TelegramBotApi

    val messageSender: TelegramMessageSender

    val failureTextMapper: TelegramFailureTextMapper
}
