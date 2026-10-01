package ru.astrainteractive.messagebridge.onboarding.impl.model

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

internal class InvalidInputError(val reply: LocalizableComponent) : Exception()

internal fun <T> refuse(reply: LocalizableComponent): Result<T> = Result.failure(InvalidInputError(reply))
