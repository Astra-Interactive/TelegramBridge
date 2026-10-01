package ru.astrainteractive.messagebridge.messenger.telegram.api

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

interface TelegramFailureTextMapper {
    fun map(failure: TelegramFailure): LocalizableComponent

    fun lazyMap(failure: TelegramFailure): LocalizableComponent
}
