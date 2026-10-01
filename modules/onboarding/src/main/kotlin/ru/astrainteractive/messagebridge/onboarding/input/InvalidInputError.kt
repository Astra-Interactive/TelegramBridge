package ru.astrainteractive.messagebridge.onboarding.input

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

/**
 * A value typed after a /mb command that cannot be saved. It is the failure of a [Result] and is never thrown.
 *
 * @property reply tells the sender what a valid value looks like
 */
internal class InvalidInputError(val reply: LocalizableComponent) : Exception()

internal fun <T> refuse(reply: LocalizableComponent): Result<T> = Result.failure(InvalidInputError(reply))
