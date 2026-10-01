@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.mapping

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.exceptions.InvalidTokenException
import net.dv8tion.jda.api.requests.CloseCode
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.Response
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.DiscordTranslation
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailureException
import ru.astrainteractive.messagebridge.messenger.discord.model.JdaShutdownException
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

    /** [InsufficientPermissionException] only reads the id of its guild. */
    private val guild = java.lang.reflect.Proxy.newProxyInstance(
        Guild::class.java.classLoader,
        arrayOf(Guild::class.java)
    ) { _, method, _ -> if (method.name == "getIdLong") 1L else null } as Guild

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
        val failure = mapper.map(JdaShutdownException(CloseCode.DISALLOWED_INTENTS.code), config)

        assertEquals(DiscordFailure.MissingIntent, failure)
        assertFalse(failure.isRetryable)
    }

    @Test
    fun GIVEN_shutdown_with_failed_authentication_WHEN_mapped_THEN_invalid_token() {
        val failure = mapper.map(JdaShutdownException(CloseCode.AUTHENTICATION_FAILED.code), config)

        assertEquals(DiscordFailure.InvalidToken, failure)
    }

    @Test
    fun GIVEN_shutdown_without_close_code_WHEN_mapped_THEN_unknown_is_retried() {
        val failure = mapper.map(JdaShutdownException(code = null), config)

        assertTrue(failure is DiscordFailure.Unknown, "$failure")
        assertTrue(failure.isRetryable)
    }

    @Test
    fun GIVEN_insufficient_permission_WHEN_mapped_THEN_permission_name_is_kept() {
        val exception = InsufficientPermissionException(guild, Permission.MANAGE_WEBHOOKS)

        val failure = mapper.map(exception, config)

        assertEquals(DiscordFailure.MissingPermission("Manage Webhooks"), failure)
        assertFalse(failure.isRetryable)
    }

    @Test
    fun GIVEN_unknown_channel_response_WHEN_mapped_THEN_configured_channel_is_not_found() {
        val exception = ErrorResponseException.create(
            ErrorResponse.UNKNOWN_CHANNEL,
            Response(null, 404, "Not Found", -1, emptySet())
        )

        assertEquals(DiscordFailure.ChannelNotFound("42"), mapper.map(exception, config))
    }

    @Test
    fun GIVEN_failure_found_by_bridge_WHEN_mapped_THEN_failure_is_kept() {
        val exception = DiscordFailureException(DiscordFailure.ChannelNotFound("7"))

        assertEquals(DiscordFailure.ChannelNotFound("7"), mapper.map(exception, config))
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
    fun GIVEN_invalid_token_WHEN_text_THEN_it_explains_how_to_reset_it() {
        assertMentions(
            mapper.toText(DiscordFailure.InvalidToken, translation),
            "Reset Token",
            "/mb discord token <token>"
        )
    }

    @Test
    fun GIVEN_missing_intent_WHEN_text_THEN_it_names_the_intent() {
        assertMentions(
            mapper.toText(DiscordFailure.MissingIntent, translation),
            "Message Content Intent",
            "Privileged Gateway Intents"
        )
    }

    @Test
    fun GIVEN_channel_not_found_WHEN_text_THEN_it_names_channel_and_next_steps() {
        assertMentions(
            mapper.toText(DiscordFailure.ChannelNotFound("42"), translation),
            "42",
            "/mb discord invite",
            "/mb discord bind"
        )
    }

    @Test
    fun GIVEN_missing_permission_WHEN_text_THEN_it_names_permission() {
        assertMentions(
            mapper.toText(DiscordFailure.MissingPermission("Manage Webhooks"), translation),
            "Manage Webhooks"
        )
    }

    @Test
    fun GIVEN_socks_proxy_WHEN_text_THEN_it_suggests_http_proxy() {
        assertMentions(
            mapper.toText(DiscordFailure.SocksNotSupported, translation),
            "/mb discord proxy http <host> <port>",
            "/mb discord proxy off"
        )
    }

    @Test
    fun GIVEN_network_error_without_proxy_WHEN_text_THEN_it_suggests_proxy() {
        val failure = DiscordFailure.Network(error = "UnknownHostException: discord.com", proxy = null)

        assertMentions(mapper.toText(failure, translation), "discord.com", "/mb discord proxy http <host> <port>")
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
}
