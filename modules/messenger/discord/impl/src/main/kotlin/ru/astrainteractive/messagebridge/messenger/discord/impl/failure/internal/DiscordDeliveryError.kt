package ru.astrainteractive.messagebridge.messenger.discord.impl.failure.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.impl.failure.mapping.DiscordFailureMapper

internal class DiscordDeliveryError(
    private val failureMapper: DiscordFailureMapper,
    private val configFlow: StateFlow<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordDeliveryError") {
    private val config: PluginConfiguration
        get() = configFlow.value
    private val translation by translationKrate

    private val lastFailureMutex = Mutex()
    private var lastFailure: DiscordFailure? = null
    private val mutableText = MutableStateFlow<LocalizableComponent?>(null)
    val text: StateFlow<LocalizableComponent?> = mutableText.asStateFlow()

    suspend fun report(t: Throwable) {
        val failure = failureMapper.map(t, config.jdaConfig)
        val text = failureMapper.toText(failure, translation.discord)
        lastFailureMutex.withLock {
            mutableText.value = text
            if (lastFailure == failure) {
                verbose { "#report ${text.toMessengerText()}" }
                return
            }
            lastFailure = failure
        }
        error { translation.discord.console.deliveryFailed(text).toMessengerText() }
        verbose { "#report ${t.stackTraceToString()}" }
    }

    suspend fun clear() {
        lastFailureMutex.withLock {
            lastFailure = null
            mutableText.value = null
        }
    }
}
