@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.proxy.command

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.ProxyType
import ru.astrainteractive.messagebridge.onboarding.fake.OnboardingFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProxyLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val setup = fixture.translation.setup

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    private fun suggestionsOf(input: String): List<String> {
        return fixture.suggestionsOf(input).map { suggestion -> suggestion.text }
    }

    @Test
    fun GIVEN_socks5_proxy_WHEN_console_sets_it_for_telegram_THEN_it_is_saved() {
        fixture.execute("mb telegram proxy socks5 1.2.3.4 1080")

        assertEquals(
            PluginConfiguration.Proxy(type = ProxyType.SOCKS5, host = "1.2.3.4", port = 1080),
            fixture.savedConfig.tgConfig.proxy
        )
        assertEquals(listOf(plain(setup.saved.proxy("SOCKS5 1.2.3.4:1080"))), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_proxy_type_in_another_spelling_WHEN_console_sets_it_THEN_it_is_read() {
        fixture.execute("mb telegram proxy SOCKS 1.2.3.4 1080")
        assertEquals(ProxyType.SOCKS5, fixture.savedConfig.tgConfig.proxy?.type)

        fixture.execute("mb telegram proxy HTTP 1.2.3.4 8080")
        assertEquals(ProxyType.HTTP, fixture.savedConfig.tgConfig.proxy?.type)
    }

    @Test
    fun GIVEN_ipv6_proxy_host_WHEN_console_sets_it_in_quotes_THEN_it_is_saved() {
        fixture.execute("mb telegram proxy http \"::1\" 8080")

        assertEquals("::1", fixture.savedConfig.tgConfig.proxy?.host)
    }

    @Test
    fun GIVEN_proxy_password_from_player_without_unsafe_WHEN_sets_proxy_THEN_is_refused() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080 user hunter2", fixture.admin)

        assertEquals(listOf(plain(setup.unsafeRequired)), fixture.repliesOf(fixture.admin))
        assertEquals(null, fixture.savedConfig.tgConfig.proxy)
    }

    @Test
    fun GIVEN_proxy_password_from_player_with_unsafe_WHEN_sets_proxy_THEN_it_is_saved_and_not_shown() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080 user hunter2 --unsafe", fixture.admin)

        val proxy = fixture.savedConfig.tgConfig.proxy
        assertEquals(PluginConfiguration.Proxy.Credentials(username = "user", password = "hunter2"), proxy?.credentials)
        assertTrue(fixture.repliesOf(fixture.admin).none { reply -> "hunter2" in reply || "user" in reply })
    }

    @Test
    fun GIVEN_proxy_password_from_console_WHEN_sets_proxy_THEN_it_is_saved() {
        fixture.execute("mb discord proxy http 1.2.3.4 8080 user hunter2")

        assertEquals("hunter2", fixture.savedConfig.jdaConfig.proxy?.password)
    }

    @Test
    fun GIVEN_proxy_username_without_password_from_player_WHEN_sets_proxy_THEN_it_is_saved_without_unsafe() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080 user", fixture.admin)

        assertEquals("user", fixture.savedConfig.tgConfig.proxy?.username)
        assertEquals(null, fixture.savedConfig.tgConfig.proxy?.password)
    }

    @Test
    fun GIVEN_more_than_a_username_and_a_password_WHEN_console_sets_proxy_THEN_is_refused() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080 user hunter2 extra")

        assertEquals(listOf(plain(setup.invalidProxyCredentials)), fixture.consoleReplies)
        assertEquals(null, fixture.savedConfig.tgConfig.proxy)
    }

    @Test
    fun GIVEN_host_with_a_scheme_WHEN_console_sets_proxy_THEN_is_refused() {
        fixture.execute("mb telegram proxy http \"http://1.2.3.4\" 8080")

        assertEquals(listOf(plain(setup.invalidHost)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_port_that_is_out_of_range_or_not_a_number_WHEN_console_sets_proxy_THEN_is_refused() {
        fixture.execute("mb telegram proxy http 1.2.3.4 70000")
        fixture.execute("mb telegram proxy http 1.2.3.4 0")
        fixture.execute("mb telegram proxy http 1.2.3.4 port")

        assertEquals(List(size = 3) { _ -> plain(setup.invalidPort) }, fixture.consoleReplies)
    }

    @Test
    fun GIVEN_unknown_proxy_type_WHEN_console_sets_proxy_THEN_is_refused() {
        fixture.execute("mb telegram proxy ftp 1.2.3.4 21")
        fixture.execute("mb discord proxy ftp 1.2.3.4 21")

        assertEquals(List(size = 2) { _ -> plain(setup.invalidProxyType) }, fixture.consoleReplies)
    }

    @Test
    fun GIVEN_proxy_WHEN_console_turns_it_off_THEN_it_is_removed() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080")

        fixture.execute("mb telegram proxy off")

        assertEquals(null, fixture.savedConfig.tgConfig.proxy)
        assertEquals(plain(setup.saved.proxyRemoved), fixture.consoleReplies.last())
    }

    @Test
    fun GIVEN_socks5_proxy_WHEN_console_sets_it_for_discord_THEN_is_refused() {
        fixture.execute("mb discord proxy socks5 1.2.3.4 1080")

        assertEquals(listOf(plain(setup.socksNotSupported)), fixture.consoleReplies)
        assertEquals(null, fixture.savedConfig.jdaConfig.proxy)
    }

    @Test
    fun GIVEN_http_proxy_WHEN_console_sets_it_for_discord_THEN_only_discord_uses_it() {
        fixture.execute("mb discord proxy http 1.2.3.4 8080")

        assertEquals(
            PluginConfiguration.Proxy(type = ProxyType.HTTP, host = "1.2.3.4", port = 8080),
            fixture.savedConfig.jdaConfig.proxy
        )
        assertEquals(null, fixture.savedConfig.tgConfig.proxy)
    }

    @Test
    fun GIVEN_proxy_type_being_typed_WHEN_console_asks_for_hints_THEN_reads_the_types_each_messenger_takes() {
        assertEquals(listOf("http", "off", "socks5"), suggestionsOf("mb telegram proxy "))
        assertEquals(listOf("http", "off"), suggestionsOf("mb discord proxy "))
    }
}
