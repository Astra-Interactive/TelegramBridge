package ru.astrainteractive.messagebridge.onboarding.impl.util

import ru.astrainteractive.astralibs.command.api.exception.LocalizableComponentCommandException
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

internal fun <T> refuse(reply: LocalizableComponent): Result<T> {
    return Result.failure(LocalizableComponentCommandException(reply))
}
