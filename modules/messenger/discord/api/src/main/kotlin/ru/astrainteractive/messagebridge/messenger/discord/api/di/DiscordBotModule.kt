package ru.astrainteractive.messagebridge.messenger.discord.api.di

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordConnection

interface DiscordBotModule {
    val connection: StateFlow<DiscordConnection>

    val deliveryError: StateFlow<LocalizableComponent?>

    val messageSender: DiscordMessageSender

    val failureTextMapper: DiscordFailureTextMapper
}
