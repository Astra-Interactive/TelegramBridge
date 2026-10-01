package ru.astrainteractive.messagebridge.messenger.telegram.api.di

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState

interface TelegramBotModule {
    val state: StateFlow<TelegramConnectionState>

    val deliveryError: StateFlow<LocalizableComponent?>

    val botApi: TelegramBotApi

    val messageSender: TelegramMessageSender

    val failureTextMapper: TelegramFailureTextMapper
}
