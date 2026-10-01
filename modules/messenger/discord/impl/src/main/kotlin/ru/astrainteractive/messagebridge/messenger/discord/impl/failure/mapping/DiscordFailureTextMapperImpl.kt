package ru.astrainteractive.messagebridge.messenger.discord.impl.failure.mapping

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure

internal class DiscordFailureTextMapperImpl(
    private val failureMapper: DiscordFailureMapper,
    private val configFlow: StateFlow<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
) : DiscordFailureTextMapper {
    private val translation by translationKrate

    override fun map(failure: DiscordFailure): LocalizableComponent = failureMapper.toText(failure, translation.discord)

    override fun mapRequestFailure(throwable: Throwable): LocalizableComponent {
        return map(failureMapper.map(throwable, configFlow.value.jdaConfig))
    }
}
