@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.impl.status.command

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.ProxyType
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.impl.fake.OnboardingFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val status = fixture.onboardingTranslation.setup.status

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    @Test
    fun GIVEN_bots_in_every_state_WHEN_console_runs_status_THEN_reads_each_bot_and_where_it_writes() {
        fixture.editConfig { config ->
            config.copy(
                tgConfig = config.tgConfig.copy(
                    chatID = "-1001234567890",
                    topicID = "12",
                    proxy = PluginConfiguration.Proxy(
                        type = ProxyType.SOCKS5,
                        host = "1.2.3.4",
                        port = 1080,
                        username = "user",
                        password = "hunter2"
                    )
                )
            )
        }
        val reason = LocalizedText.shared("the token is revoked")
        val deliveryError = LocalizedText.shared("the bot can not write to the channel")
        fixture.telegram.status.value = MessengerStatus.Connected("@ServerBot")
        fixture.discord.status.value = MessengerStatus.Failed(reason)
        fixture.discord.deliveryError.value = deliveryError

        fixture.execute("mb status")

        assertEquals(
            listOf(
                plain(status.header),
                "Telegram: connected as @ServerBot",
                "  Chat: -1001234567890, topic 12",
                "  Proxy: SOCKS5 1.2.3.4:1080",
                "Discord: not connected — the token is revoked",
                plain(status.noChannel),
                plain(status.noProxy),
                plain(status.deliveryError(deliveryError))
            ),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_bots_that_are_connecting_WHEN_console_runs_status_THEN_reads_the_chat_the_channel_and_the_mirror() {
        fixture.editConfig { config ->
            config.copy(
                tgConfig = config.tgConfig.copy(
                    chatID = "-1001234567890",
                    apiUrl = "https://user:secret@tg.example.com"
                ),
                jdaConfig = config.jdaConfig.copy(
                    channelId = "123456789012345678",
                    proxy = PluginConfiguration.Proxy(type = ProxyType.HTTP, host = "proxy.example.com", port = 8080)
                )
            )
        }
        fixture.telegram.status.value = MessengerStatus.Connecting
        fixture.discord.status.value = MessengerStatus.Connecting

        fixture.execute("mb status")

        assertEquals(
            listOf(
                plain(status.header),
                "Telegram: connecting…",
                "  Chat: -1001234567890",
                plain(status.noProxy),
                "  Bot API: https://tg.example.com",
                "Discord: connecting…",
                "  Channel: 123456789012345678",
                "  Proxy: HTTP proxy.example.com:8080"
            ),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_bots_without_tokens_WHEN_console_runs_status_THEN_reads_how_to_set_each_token() {
        fixture.execute("mb status")

        assertEquals(
            listOf(
                plain(status.header),
                "Telegram: not configured — /mb telegram token <token>",
                plain(status.noChat),
                plain(status.noProxy),
                "Discord: not configured — /mb discord token <token>",
                plain(status.noChannel),
                plain(status.noProxy)
            ),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_player_without_permissions_WHEN_runs_status_THEN_reads_no_permission() {
        fixture.execute("mb status", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
    }
}
