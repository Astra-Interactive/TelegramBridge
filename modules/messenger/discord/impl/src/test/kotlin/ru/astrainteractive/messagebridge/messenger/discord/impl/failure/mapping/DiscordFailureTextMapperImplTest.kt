@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.impl.failure.mapping

import kotlinx.coroutines.flow.MutableStateFlow
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.impl.failure.model.DiscordFailureError
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordFailureTextMapperImplTest {
    private val translation = PluginTranslation()
    private val mapper = DiscordFailureTextMapperImpl(
        failureMapper = DiscordFailureMapper(),
        configFlow = MutableStateFlow(PluginConfiguration()),
        translationKrate = FakeTranslationKrate(translation)
    )

    @Test
    fun GIVEN_failure_WHEN_mapped_THEN_it_is_the_text_of_the_discord_errors() {
        val text = mapper.map(DiscordFailure.InvalidToken)

        assertEquals(translation.discord.errors.invalidToken.toMessengerText(), text.toMessengerText())
    }

    @Test
    fun GIVEN_failed_request_WHEN_mapped_THEN_it_is_the_text_of_its_failure() {
        val text = mapper.mapRequestFailure(DiscordFailureError(DiscordFailure.ChannelNotSet))

        assertEquals(translation.discord.errors.channelNotSet.toMessengerText(), text.toMessengerText())
    }
}
