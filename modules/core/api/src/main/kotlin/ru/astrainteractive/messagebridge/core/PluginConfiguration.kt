package ru.astrainteractive.messagebridge.core

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PluginConfiguration(
    @YamlComment(
        "MessageBridge settings. Change them with the /mb commands, or edit this file and run /mb reload.",
        "The file is rewritten on every load, so comments you add here are lost.",
        "Discord bot. Run /mb discord for a step-by-step guide.",
        "Docs: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/discord.md"
    )
    val jdaConfig: JdaConfig = JdaConfig(),
    @YamlComment(
        "Telegram bot. Run /mb telegram for a step-by-step guide.",
        "Docs: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/telegram.md"
    )
    val tgConfig: TelegramConfig = TelegramConfig(),
    @YamlComment("Send a message to Telegram and Discord when a player joins the server.")
    val displayJoinMessage: Boolean = true,
    @YamlComment("Send a message to Telegram and Discord when a player leaves the server.")
    val displayLeaveMessage: Boolean = true,
    @YamlComment("Send death messages to Telegram and Discord.")
    val displayDeathMessage: Boolean = true,
    @YamlComment(
        "Roles given to a player who links an account with /link. null gives no roles.",
        "Example:",
        "link:",
        "  linkDiscordRole: \"123456789012345678\"",
        "  linkLuckPermsRole: \"verified\""
    )
    val link: Link? = null
) {
    @Serializable
    data class Link(
        @YamlComment(
            "Id of the Discord role given on linking a Discord account.",
            "With Developer Mode on: Server Settings -> Roles -> ... next to the role -> Copy Role ID.",
            "The bot needs Manage Roles, and its own role must be above this role."
        )
        val linkDiscordRole: String,
        @YamlComment("LuckPerms group given on linking a Telegram or Discord account, e.g. verified.")
        val linkLuckPermsRole: String
    )

    @Serializable
    data class Proxy(
        @YamlComment("HTTP or SOCKS5. Discord works only through HTTP. SOCKS5 works for Telegram without a password.")
        val type: ProxyType = ProxyType.HTTP,
        @YamlComment("Address of the proxy, e.g. 127.0.0.1 for a proxy client on the same machine.")
        val host: String,
        @YamlComment("Port of the proxy. Local proxy clients often have separate SOCKS5 and HTTP ports.")
        val port: Int,
        @YamlComment("Login of the proxy, null when it needs none.")
        val username: String? = null,
        @YamlComment("Password of the proxy, null when it needs none. Keep it private.")
        val password: String? = null
    ) {
        /** `null` when the proxy needs no authentication. */
        val credentials: Credentials?
            get() = username
                ?.takeIf(String::isNotBlank)
                ?.let { name -> Credentials(username = name, password = password.orEmpty()) }

        data class Credentials(
            val username: String,
            val password: String
        )
    }

    @Serializable
    data class JdaConfig(
        @YamlComment(
            "Bot token: https://discord.com/developers/applications -> your app -> Bot -> Reset Token.",
            "Set it with /mb discord token <token>. Empty turns Discord off.",
            "Keep it private: anyone who has the token controls the bot."
        )
        val token: String = "",
        @YamlComment("Status of the bot, shown as \"Playing <activity>\". Set it with /mb discord activity <text>.")
        val activity: String = "",
        @YamlComment(
            "Id of the text channel to relay. /mb discord bind fills it in for you.",
            "Or turn on Developer Mode in Discord and right-click the channel -> Copy Channel ID,",
            "then run /mb discord channel <channel_id>."
        )
        val channelId: String = "",
        @YamlComment(
            "HTTP proxy for Discord, null for a direct connection.",
            "Set it with /mb discord proxy http <host> <port>, remove it with /mb discord proxy off."
        )
        val proxy: Proxy? = null
    )

    @Serializable
    data class TelegramConfig(
        @YamlComment(
            "Bot token from @BotFather (/newbot), looks like 123456789:AA...",
            "Set it with /mb telegram token <token>. Empty turns Telegram off.",
            "Keep it private: anyone who has the token controls the bot."
        )
        @SerialName("token")
        val token: String = "",
        @YamlComment(
            "Id of the group, looks like -1001234567890. /mb telegram bind fills it in for you.",
            "Or send /minfo in the group and run /mb telegram chat <chat_id>."
        )
        @SerialName("chat_id")
        val chatID: String = "",
        @YamlComment(
            "Id of the forum topic to relay. Empty: a group without topics, or the General topic.",
            "/mb telegram bind sent inside a topic fills it in. Set it with /mb telegram topic <topic_id|none>."
        )
        @SerialName("topic_id")
        val topicID: String = "",
        @YamlComment("Longer Telegram messages are deleted and not relayed to Minecraft.")
        @SerialName("max_telegram_message_length")
        val maxTelegramMessageLength: Int = 90,
        @YamlComment(
            "Regex for the names of Telegram users who have no @username. .* allows every name.",
            "Messages from names that do not match are deleted."
        )
        @SerialName("display_name_regex")
        val displayNameRegex: String = ".*",
        @YamlComment(
            "Proxy for Telegram, null for a direct connection.",
            "Set it with /mb telegram proxy <http|socks5> <host> <port>, remove it with /mb telegram proxy off."
        )
        @SerialName("proxy")
        val proxy: Proxy? = null,
        @YamlComment(
            "Bot API server, empty for https://api.telegram.org. Set it with /mb telegram api-url <url|default>.",
            "Use only a server you run yourself: whoever runs it sees the token."
        )
        @SerialName("api_url")
        val apiUrl: String = ""
    )
}
