@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.failure.mapping

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.config.ProxyType
import ru.astrainteractive.messagebridge.core.api.config.TelegramTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TelegramFailureTextMapperImplTest {
    private val translation = PluginTranslation()
    private val errors = translation.telegram.errors
    private val translationKrate = FakeTranslationKrate(translation)
    private val mapper = TelegramFailureTextMapperImpl(translationKrate = translationKrate)
    private val proxy = PluginConfiguration.Proxy(type = ProxyType.HTTP, host = "127.0.0.1", port = 3128)

    private fun assertText(expected: LocalizableComponent, failure: TelegramFailure) {
        assertEquals(expected.toMessengerText(), mapper.map(failure).toMessengerText(), "$failure")
    }

    @Test
    fun GIVEN_failures_without_details_WHEN_mapped_THEN_each_has_its_own_text() {
        assertText(errors.invalidToken, TelegramFailure.InvalidToken)
        assertText(errors.chatNotSet, TelegramFailure.ChatNotSet)
        assertText(errors.chatNotFound, TelegramFailure.ChatNotFound)
        assertText(errors.topicNotFound, TelegramFailure.TopicNotFound)
        assertText(errors.botNotInChat, TelegramFailure.BotNotInChat)
        assertText(errors.noRights, TelegramFailure.NoRights)
        assertText(errors.tokenInUse, TelegramFailure.TokenInUse)
        assertText(errors.proxyAuth, TelegramFailure.ProxyAuth)
        assertText(errors.invalidApiUrl, TelegramFailure.InvalidApiUrl)
        assertText(errors.socksWithPassword, TelegramFailure.SocksWithPassword)
    }

    @Test
    fun GIVEN_migrated_chat_WHEN_mapped_THEN_text_names_the_new_id() {
        val text = mapper.map(TelegramFailure.ChatMigrated(-1009876543210)).toMessengerText()

        assertTrue("-1009876543210" in text, text)
    }

    @Test
    fun GIVEN_rate_limit_with_delay_WHEN_mapped_THEN_text_names_the_seconds() {
        assertText(errors.rateLimitedFor(7.seconds), TelegramFailure.RateLimited(retryAfter = 7.seconds))
        assertTrue("7" in mapper.map(TelegramFailure.RateLimited(retryAfter = 7.seconds)).toMessengerText())
    }

    @Test
    fun GIVEN_rate_limit_without_delay_WHEN_mapped_THEN_text_has_no_seconds() {
        assertText(errors.rateLimited, TelegramFailure.RateLimited(retryAfter = null))
    }

    @Test
    fun GIVEN_server_error_WHEN_mapped_THEN_text_names_the_code() {
        assertText(errors.serverError(502), TelegramFailure.ServerError(502))
    }

    @Test
    fun GIVEN_network_through_proxy_WHEN_mapped_THEN_text_names_the_proxy() {
        val text = mapper.map(TelegramFailure.Network(proxy = proxy, apiUrl = "https://tg.example.com"))

        assertEquals(errors.networkProxy("127.0.0.1:3128").toMessengerText(), text.toMessengerText())
    }

    @Test
    fun GIVEN_network_to_own_server_WHEN_mapped_THEN_text_names_the_server() {
        val failure = TelegramFailure.Network(proxy = null, apiUrl = "https://tg.example.com")

        assertText(errors.networkApiUrl("https://tg.example.com"), failure)
    }

    @Test
    fun GIVEN_network_to_telegram_WHEN_mapped_THEN_text_suggests_a_proxy() {
        assertText(errors.network, TelegramFailure.Network(proxy = null, apiUrl = null))
    }

    @Test
    fun GIVEN_invalid_proxy_WHEN_mapped_THEN_text_names_its_address() {
        val invalidProxy = PluginConfiguration.Proxy(host = "proxy.local", port = 70_000)

        assertText(errors.invalidProxy("proxy.local:70000"), TelegramFailure.InvalidProxy(invalidProxy))
    }

    @Test
    fun GIVEN_unknown_failure_WHEN_mapped_THEN_text_keeps_the_message() {
        val text = mapper.map(TelegramFailure.Unknown("Bad Request: message text is empty")).toMessengerText()

        assertTrue("Bad Request: message text is empty" in text, text)
    }

    @Test
    fun GIVEN_lazy_text_WHEN_translation_is_reloaded_THEN_text_reads_the_new_translation() {
        val text = mapper.lazyMap(TelegramFailure.InvalidToken)
        val reloaded = LocalizedText.build { translation(MinecraftLocales.EN_US, "Reloaded token text") }

        translationKrate.translation = translation.copy(
            telegram = translation.telegram.copy(
                errors = TelegramTranslation.Errors(invalidToken = reloaded)
            )
        )

        assertEquals("Reloaded token text", text.toMessengerText())
    }
}
