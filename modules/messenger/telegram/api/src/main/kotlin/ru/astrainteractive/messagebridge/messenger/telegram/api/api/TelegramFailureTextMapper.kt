package ru.astrainteractive.messagebridge.messenger.telegram.api.api

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramFailure

interface TelegramFailureTextMapper {
    fun map(failure: TelegramFailure): LocalizableComponent

    fun lazyMap(failure: TelegramFailure): LocalizableComponent
}
