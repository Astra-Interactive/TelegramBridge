package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import java.util.concurrent.atomic.AtomicReference

/**
 * Why the last message did not reach Discord. A new reason is logged once, a repeated one only in verbose, so a
 * broken channel does not flood the console with every chat message.
 */
internal class DiscordDeliveryError(
    private val failureMapper: DiscordFailureMapper,
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordDelivery") {
    private val config by configKrate
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
            error { "Messages cannot be delivered to Discord: ${text.toMessengerText()}" }
            verbose { "#report ${throwable.stackTraceToString()}" }
        }
    }

    fun clear() {
        lastFailure.set(null)
        mutableText.value = null
    }
}
