package ru.astrainteractive.messagebridge.messenger.discord.di

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection

interface DiscordBotModule {
    val connection: StateFlow<DiscordConnection>

    val deliveryError: StateFlow<LocalizableComponent?>

    val messageSender: DiscordMessageSender

    val failureTextMapper: DiscordFailureTextMapper
}
