package ru.astrainteractive.messagebridge.messenger.discord.api

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure

interface DiscordFailureTextMapper {
    fun map(failure: DiscordFailure): LocalizableComponent

    fun mapRequestFailure(t: Throwable): LocalizableComponent
}
