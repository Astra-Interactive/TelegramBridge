@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.impl.failure.mapping

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.exceptions.InvalidTokenException
import net.dv8tion.jda.api.requests.CloseCode
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.Response
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.config.DiscordTranslation
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.impl.failure.model.DiscordFailureError
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.CompletionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscordFailureMapperTest {
    private val mapper = DiscordFailureMapper()
    private val translation = DiscordTranslation()
    private val config = PluginConfiguration.JdaConfig(token = "token", channelId = "42")
    private val proxy = PluginConfiguration.Proxy(host = "10.0.0.1", port = 3128)

    private val guild = jdaFake<Guild> { method, _ -> if (method.name == "getIdLong") 1L else null }

    private fun networkError(cause: Exception): ErrorResponseException {
        return ErrorResponseException.create(ErrorResponse.SERVER_ERROR, Response(cause, emptySet()))
    }

    private fun text(failure: DiscordFailure): String {
        return mapper.toText(failure, translation).toMessengerText()
    }

    private fun assertMentions(text: LocalizableComponent, vararg parts: String) {
        val plainText = text.toMessengerText()
        parts.forEach { part -> assertTrue(part in plainText, plainText) }
    }

    @Test
    fun GIVEN_invalid_token_exception_WHEN_mapped_THEN_invalid_token_is_not_retried() {
        val failure = mapper.map(InvalidTokenException(), config)

        assertEquals(DiscordFailure.InvalidToken, failure)
        assertFalse(failure.isRetryable)
    }

    @Test
    fun GIVEN_invalid_token_inside_completion_exception_WHEN_mapped_THEN_invalid_token() {
        val failure = mapper.map(CompletionException(InvalidTokenException()), config)

        assertEquals(DiscordFailure.InvalidToken, failure)
    }

    @Test
    fun GIVEN_shutdown_with_disallowed_intents_WHEN_mapped_THEN_missing_intent_is_not_retried() {
        val failure = mapper.mapShutdown(CloseCode.DISALLOWED_INTENTS.code, INTENTS)

        assertEquals(DiscordFailure.MissingIntent(INTENTS), failure)
        assertFalse(failure.isRetryable)
    }

    @Test
    fun GIVEN_shutdown_with_failed_authentication_WHEN_mapped_THEN_invalid_token() {
        val failure = mapper.mapShutdown(CloseCode.AUTHENTICATION_FAILED.code, INTENTS)

        assertEquals(DiscordFailure.InvalidToken, failure)
    }

    @Test
    fun GIVEN_shutdown_without_close_code_WHEN_mapped_THEN_unknown_is_retried() {
        val failure = mapper.mapShutdown(closeCode = null, privilegedIntents = INTENTS)

        assertTrue(failure is DiscordFailure.Unknown, "$failure")
        assertTrue(failure.isRetryable)
    }

    @Test
    fun GIVEN_insufficient_permission_WHEN_mapped_THEN_permission_name_is_kept() {
        val t = InsufficientPermissionException(guild, Permission.MANAGE_WEBHOOKS)

        val failure = mapper.map(t, config)

        assertEquals(DiscordFailure.MissingPermission("Manage Webhooks"), failure)
        assertFalse(failure.isRetryable)
    }

    @Test
    fun GIVEN_unknown_channel_response_WHEN_mapped_THEN_configured_channel_is_not_found() {
        val t = ErrorResponseException.create(
            ErrorResponse.UNKNOWN_CHANNEL,
            Response(null, 404, "Not Found", -1, emptySet())
        )

        assertEquals(DiscordFailure.ChannelNotFound("42"), mapper.map(t, config))
    }

    @Test
    fun GIVEN_failure_found_by_bridge_WHEN_mapped_THEN_failure_is_kept() {
        val t = DiscordFailureError(DiscordFailure.ChannelNotFound("7"))

        assertEquals(DiscordFailure.ChannelNotFound("7"), mapper.map(t, config))
    }

    @Test
    fun GIVEN_dns_error_without_proxy_WHEN_mapped_THEN_network_without_proxy_is_retried() {
        val failure = mapper.map(networkError(UnknownHostException("discord.com")), config)

        assertEquals(DiscordFailure.Network(error = "UnknownHostException: discord.com", proxy = null), failure)
        assertTrue(failure.isRetryable)
    }

    @Test
    fun GIVEN_connection_error_with_proxy_WHEN_mapped_THEN_network_names_proxy() {
        val failure = mapper.map(networkError(ConnectException("Connection refused")), config.copy(proxy = proxy))

        assertEquals(
            DiscordFailure.Network(error = "ConnectException: Connection refused", proxy = "10.0.0.1:3128"),
            failure
        )
    }

    @Test
    fun GIVEN_unexpected_exception_WHEN_mapped_THEN_unknown_keeps_message() {
        val failure = mapper.map(IllegalStateException("boom"), config)

        assertEquals(DiscordFailure.Unknown("IllegalStateException: boom"), failure)
    }

    @Test
    fun GIVEN_invalid_token_WHEN_text_THEN_reads_invalid_token() {
        assertEquals(translation.errors.invalidToken.toMessengerText(), text(DiscordFailure.InvalidToken))
    }

    @Test
    fun GIVEN_missing_intents_WHEN_text_THEN_it_names_every_intent_to_turn_on() {
        assertMentions(
            mapper.toText(DiscordFailure.MissingIntent(INTENTS), translation),
            "Message Content Intent, Server Members Intent"
        )
    }

    @Test
    fun GIVEN_close_code_unknown_to_jda_WHEN_mapped_THEN_unknown_keeps_the_code() {
        val failure = mapper.mapShutdown(closeCode = 4999, privilegedIntents = INTENTS)

        assertEquals(DiscordFailure.Unknown("JDA stopped with close code 4999"), failure)
    }

    @Test
    fun GIVEN_channel_not_found_WHEN_text_THEN_it_names_channel() {
        assertMentions(mapper.toText(DiscordFailure.ChannelNotFound("42"), translation), "42")
    }

    @Test
    fun GIVEN_missing_permission_WHEN_text_THEN_it_names_permission() {
        assertMentions(
            mapper.toText(DiscordFailure.MissingPermission("Manage Webhooks"), translation),
            "Manage Webhooks"
        )
    }

    @Test
    fun GIVEN_socks_proxy_WHEN_text_THEN_reads_socks_not_supported() {
        assertEquals(translation.errors.socksNotSupported.toMessengerText(), text(DiscordFailure.SocksNotSupported))
    }

    @Test
    fun GIVEN_network_error_without_proxy_WHEN_text_THEN_it_names_the_error() {
        val failure = DiscordFailure.Network(error = "UnknownHostException: discord.com", proxy = null)

        assertMentions(mapper.toText(failure, translation), "UnknownHostException: discord.com")
    }

    @Test
    fun GIVEN_network_error_with_proxy_WHEN_text_THEN_it_names_proxy() {
        val failure = DiscordFailure.Network(error = "ConnectException: Connection refused", proxy = "10.0.0.1:3128")

        assertMentions(mapper.toText(failure, translation), "10.0.0.1:3128", "Connection refused")
    }

    @Test
    fun GIVEN_error_with_markup_WHEN_text_THEN_markup_is_shown_as_text() {
        val failure = DiscordFailure.Unknown("<red>&cboom")

        assertTrue("<red>&cboom" in text(failure), text(failure))
    }

    private companion object {
        val INTENTS = listOf("Message Content Intent", "Server Members Intent")
    }
}
