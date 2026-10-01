package ru.astrainteractive.messagebridge.messenger.discord.api.api

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure

interface DiscordFailureTextMapper {
    fun map(failure: DiscordFailure): LocalizableComponent

    fun mapRequestFailure(throwable: Throwable): LocalizableComponent
}
