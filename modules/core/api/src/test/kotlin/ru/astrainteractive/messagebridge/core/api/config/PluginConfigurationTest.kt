@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.api.config

import com.charleskorn.kaml.Yaml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PluginConfigurationTest {
    private val yaml = Yaml(configuration = Yaml.default.configuration.copy(encodeDefaults = true, strictMode = false))

    private val configured = PluginConfiguration(
        jdaConfig = PluginConfiguration.JdaConfig(
            token = "discord-token",
            activity = "play.example.com",
            channelId = "123456789012345678",
            proxy = PluginConfiguration.Proxy(host = "127.0.0.1", port = 10809, username = "user", password = "pass")
        ),
        tgConfig = PluginConfiguration.TelegramConfig(
            token = "123456789:AAtoken",
            chatID = "-1001234567890",
            topicID = "12",
            proxy = PluginConfiguration.Proxy(type = ProxyType.SOCKS5, host = "::1", port = 1080),
            apiUrl = "https://bot-api.example.com"
        ),
        displayDeathMessage = false,
        link = PluginConfiguration.Link(linkDiscordRole = "987654321098765432", linkLuckPermsRole = "verified")
    )

    private fun encode(configuration: PluginConfiguration): String {
        return yaml.encodeToString(PluginConfiguration.serializer(), configuration)
    }

    private fun decode(text: String): PluginConfiguration {
        return yaml.decodeFromString(PluginConfiguration.serializer(), text)
    }

    @Test
    fun GIVEN_defaults_WHEN_encoded_THEN_top_level_settings_have_comments() {
        val text = encode(PluginConfiguration())

        assertTrue(text.lines().first().startsWith("# MessageBridge settings"), text)
        assertTrue("# Send death messages to Telegram and Discord." in text, text)
    }

    @Test
    fun GIVEN_defaults_WHEN_encoded_THEN_nested_settings_have_indented_comments() {
        val lines = encode(PluginConfiguration()).lines()

        val tokenComment = lines.indexOfFirst { line -> line.startsWith("  # Bot token from @BotFather") }
        assertTrue(tokenComment >= 0, lines.joinToString("\n"))
        val nextSetting = lines.drop(tokenComment).first { line -> !line.trimStart().startsWith("#") }
        assertEquals("  token: \"\"", nextSetting)
    }

    @Test
    fun GIVEN_proxy_WHEN_encoded_THEN_proxy_settings_have_comments() {
        val text = encode(configured)

        assertTrue("    # HTTP or SOCKS5." in text, text)
        assertTrue("    # Password of the proxy, null when it needs none. Keep it private." in text, text)
    }

    @Test
    fun GIVEN_defaults_WHEN_encoded_and_decoded_THEN_defaults_are_returned() {
        assertEquals(PluginConfiguration(), decode(encode(PluginConfiguration())))
    }

    @Test
    fun GIVEN_every_setting_filled_WHEN_encoded_and_decoded_THEN_same_settings_are_returned() {
        assertEquals(configured, decode(encode(configured)))
    }
}
