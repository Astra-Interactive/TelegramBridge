package ru.astrainteractive.messagebridge.messenger.discord.impl.failure.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.impl.failure.mapping.DiscordFailureMapper
import java.util.concurrent.atomic.AtomicReference

internal class DiscordDeliveryError(
    private val failureMapper: DiscordFailureMapper,
    private val configFlow: StateFlow<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordDelivery") {
    private val config: PluginConfiguration
        get() = configFlow.value
    private val translation by translationKrate

    private val lastFailure = AtomicReference<DiscordFailure?>(null)
    private val mutableText = MutableStateFlow<LocalizableComponent?>(null)
    val text: StateFlow<LocalizableComponent?> = mutableText.asStateFlow()

    fun report(throwable: Throwable) {
        val failure = failureMapper.map(throwable, config.jdaConfig)
        val text = failureMapper.toText(failure, translation.discord)
        mutableText.value = text
        if (lastFailure.getAndSet(failure) == failure) {
            verbose { "#report ${text.toMessengerText()}" }
        } else {
            error { translation.discord.console.deliveryFailed(text).toMessengerText() }
            verbose { "#report ${throwable.stackTraceToString()}" }
        }
    }

    fun clear() {
        lastFailure.set(null)
        mutableText.value = null
    }
}
