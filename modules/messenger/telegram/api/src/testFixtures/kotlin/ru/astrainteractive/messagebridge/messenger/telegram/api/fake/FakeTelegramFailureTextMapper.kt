package ru.astrainteractive.messagebridge.messenger.telegram.api.fake

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramFailure

class FakeTelegramFailureTextMapper : TelegramFailureTextMapper {
    override fun map(failure: TelegramFailure): LocalizableComponent = LocalizedText.shared("Telegram failed: $failure")

    override fun lazyMap(failure: TelegramFailure): LocalizableComponent = map(failure)
}
