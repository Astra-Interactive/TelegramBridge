package ru.astrainteractive.messagebridge.core.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

@Serializable
data class DiscordTranslation(
    @SerialName("errors")
    val errors: Errors = Errors(),
    @SerialName("chat")
    val chat: Chat = Chat(),
    @SerialName("console")
    val console: Console = Console()
) {
    @Serializable
    data class Chat(
        @SerialName("player_joined")
        private val playerJoined: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%player% joined the server")
            translation(MinecraftLocales.RU_RU, "%player% зашёл на сервер")
        },
        @SerialName("player_joined_first_time")
        private val playerJoinedFirstTime: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%player% joined the server for the first time!")
            translation(MinecraftLocales.RU_RU, "%player% впервые зашёл на сервер!")
        },
        @SerialName("player_left")
        private val playerLeft: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%player% left the server")
            translation(MinecraftLocales.RU_RU, "%player% вышел с сервера")
        },
        @SerialName("player_died")
        private val playerDied: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%player% died")
            translation(MinecraftLocales.RU_RU, "%player% погиб")
        },
        @SerialName("server_started")
        val serverStarted: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "✅ **The server has started**")
            translation(MinecraftLocales.RU_RU, "✅ **Сервер запущен**")
        },
        @SerialName("server_stopped")
        val serverStopped: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "🛑 **The server has stopped**")
            translation(MinecraftLocales.RU_RU, "🛑 **Сервер остановлен**")
        },
        @SerialName("topic_online")
        private val topicOnline: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Players online: %count%")
            translation(MinecraftLocales.RU_RU, "Игроков в сети: %count%")
        },
        @SerialName("topic_starting")
        val topicStarting: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The server has just started...")
            translation(MinecraftLocales.RU_RU, "Сервер только что запустился...")
        },
        @SerialName("topic_stopped")
        val topicStopped: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The server is stopped")
            translation(MinecraftLocales.RU_RU, "Сервер остановлен")
        }
    ) {
        fun playerJoined(player: String): LocalizableComponent = playerJoined.replaceAll(
            PlaceholderReplacement.plain("%player%", player)
        )

        fun playerJoinedFirstTime(player: String): LocalizableComponent = playerJoinedFirstTime.replaceAll(
            PlaceholderReplacement.plain("%player%", player)
        )

        fun playerLeft(player: String): LocalizableComponent = playerLeft.replaceAll(
            PlaceholderReplacement.plain("%player%", player)
        )

        fun playerDied(player: String): LocalizableComponent = playerDied.replaceAll(
            PlaceholderReplacement.plain("%player%", player)
        )

        fun topicOnline(count: Int): LocalizableComponent = topicOnline.replaceAll(
            PlaceholderReplacement.plain("%count%", "$count")
        )
    }

    @Serializable
    data class Console(
        @SerialName("disabled")
        val disabled: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Discord is turned off: the bot token is empty")
            translation(MinecraftLocales.RU_RU, "Discord выключен: токен бота не задан")
        },
        @SerialName("connected")
        private val connected: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Discord bot %bot% is connected")
            translation(MinecraftLocales.RU_RU, "Бот Discord %bot% подключён")
        },
        @SerialName("not_connected")
        private val notConnected: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Discord bot is not connected: %reason%")
            translation(MinecraftLocales.RU_RU, "Бот Discord не подключён: %reason%")
        },
        @SerialName("delivery_failed")
        private val deliveryFailed: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Messages cannot be delivered to Discord: %reason%")
            translation(MinecraftLocales.RU_RU, "Сообщения не доходят до Discord: %reason%")
        }
    ) {
        fun connected(bot: String): LocalizableComponent = connected.replaceAll(
            PlaceholderReplacement.plain("%bot%", bot)
        )

        fun notConnected(reason: LocalizableComponent): LocalizableComponent = notConnected.replaceAll(
            PlaceholderReplacement(placeholder = "%reason%", value = reason)
        )

        fun deliveryFailed(reason: LocalizableComponent): LocalizableComponent = deliveryFailed.replaceAll(
            PlaceholderReplacement(placeholder = "%reason%", value = reason)
        )
    }

    @Serializable
    data class Errors(
        @SerialName("invalid_token")
        val invalidToken: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Discord rejected the bot token. Open https://discord.com/developers/applications, choose your " +
                    "application, open Bot, press Reset Token and run /mb discord token <token> with the new token"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Discord не принял токен бота. Откройте https://discord.com/developers/applications, выберите " +
                    "приложение, откройте Bot, нажмите Reset Token и выполните /mb discord token <токен> " +
                    "с новым токеном"
            )
        },
        @SerialName("missing_intents")
        private val missingIntents: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Discord refused the connection because an intent the bot needs is off. Open " +
                    "https://discord.com/developers/applications, choose your application, open Bot, turn on " +
                    "%intents% under Privileged Gateway Intents, save the changes and run /mb reload"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Discord не пустил бота: выключен нужный ему intent. Откройте " +
                    "https://discord.com/developers/applications, выберите приложение, откройте Bot, включите " +
                    "%intents% в разделе Privileged Gateway Intents, сохраните изменения и выполните /mb reload"
            )
        },
        @SerialName("channel_not_set")
        val channelNotSet: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The Discord channel is not set. Run /mb discord bind and send the code into the channel, " +
                    "or set its id with /mb discord channel <channel_id>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Канал Discord не задан. Выполните /mb discord bind и отправьте код в нужный канал " +
                    "или укажите его id: /mb discord channel <id_канала>"
            )
        },
        @SerialName("channel_not_found")
        private val channelNotFound: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot cannot find the channel %channel_id%: the bot is not on that server or the id is wrong. " +
                    "Add the bot with /mb discord invite, then run /mb discord bind and send the code into the channel"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бот не видит канал %channel_id%: бота нет на этом сервере или id указан неверно. " +
                    "Добавьте бота через /mb discord invite, затем выполните /mb discord bind и отправьте код " +
                    "в нужный канал"
            )
        },
        @SerialName("missing_permission")
        private val missingPermission: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot lacks the \"%permission%\" permission in the channel. Give it to the bot's role " +
                    "in the channel settings or add the bot again with /mb discord invite"
            )
            translation(
                MinecraftLocales.RU_RU,
                "У бота нет права «%permission%» в канале. Выдайте его роли бота в настройках канала " +
                    "или добавьте бота заново через /mb discord invite"
            )
        },
        @SerialName("socks_not_supported")
        val socksNotSupported: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Discord works only through an HTTP proxy, but a SOCKS5 proxy is set. Set an HTTP proxy with " +
                    "/mb discord proxy http <host> <port> [username] [password] or turn the proxy off " +
                    "with /mb discord proxy off"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Discord работает только через HTTP-прокси, а указан SOCKS5. Задайте HTTP-прокси: " +
                    "/mb discord proxy http <хост> <порт> [логин] [пароль] или отключите прокси: " +
                    "/mb discord proxy off"
            )
        },
        @SerialName("network")
        private val network: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Could not reach Discord: %error%. If Discord is blocked in your country, set an HTTP proxy " +
                    "with /mb discord proxy http <host> <port> [username] [password]. How to get one: " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/proxy.md"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удалось подключиться к Discord: %error%. Если Discord заблокирован в вашей стране, " +
                    "задайте HTTP-прокси: /mb discord proxy http <хост> <порт> [логин] [пароль]. " +
                    "Где его взять (на английском): " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/proxy.md"
            )
        },
        @SerialName("network_via_proxy")
        private val networkViaProxy: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Could not reach Discord through the proxy %proxy%: %error%. Check that the proxy works and " +
                    "its address, username and password are right, or change it with " +
                    "/mb discord proxy http <host> <port> [username] [password]"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удалось подключиться к Discord через прокси %proxy%: %error%. Проверьте, что прокси " +
                    "работает, а адрес, логин и пароль указаны верно, или замените его: " +
                    "/mb discord proxy http <хост> <порт> [логин] [пароль]"
            )
        },
        @SerialName("unknown")
        private val unknown: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Discord error: %error%. Common problems and their fixes: " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/troubleshooting.md"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Ошибка Discord: %error%. Частые проблемы и их решения (на английском): " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/troubleshooting.md"
            )
        }
    ) {
        fun missingIntents(intents: String): LocalizableComponent = missingIntents.replaceAll(
            PlaceholderReplacement.plain("%intents%", intents)
        )

        fun channelNotFound(channelId: String): LocalizableComponent = channelNotFound.replaceAll(
            PlaceholderReplacement.plain("%channel_id%", channelId)
        )

        fun missingPermission(permission: String): LocalizableComponent = missingPermission.replaceAll(
            PlaceholderReplacement.plain("%permission%", permission)
        )

        fun network(error: String): LocalizableComponent = network.replaceAll(
            PlaceholderReplacement.plain("%error%", error)
        )

        fun networkViaProxy(proxy: String, error: String): LocalizableComponent = networkViaProxy.replaceAll(
            PlaceholderReplacement.plain("%proxy%", proxy),
            PlaceholderReplacement.plain("%error%", error)
        )

        fun unknown(error: String): LocalizableComponent = unknown.replaceAll(
            PlaceholderReplacement.plain("%error%", error)
        )
    }
}
