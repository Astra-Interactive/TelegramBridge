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
            translation(MinecraftLocales.EN_US, "Discord rejected the bot token")
            translation(MinecraftLocales.RU_RU, "Discord не принял токен бота")
        },
        @SerialName("missing_intents")
        private val missingIntents: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Privileged intents are off in the Developer Portal: %intents%")
            translation(MinecraftLocales.RU_RU, "В Developer Portal выключены привилегированные intents: %intents%")
        },
        @SerialName("channel_not_set")
        val channelNotSet: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "channelId is not set")
            translation(MinecraftLocales.RU_RU, "channelId не задан")
        },
        @SerialName("channel_not_found")
        private val channelNotFound: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Channel %channel_id% not found: the id is wrong or the bot is not on that server"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Канал %channel_id% не найден: id неверный или бота нет на этом сервере"
            )
        },
        @SerialName("missing_permission")
        private val missingPermission: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, """The bot lacks the "%permission%" permission in the channel""")
            translation(MinecraftLocales.RU_RU, "У бота нет права «%permission%» в канале")
        },
        @SerialName("socks_not_supported")
        val socksNotSupported: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Discord works only through an HTTP proxy, but a SOCKS5 proxy is set")
            translation(MinecraftLocales.RU_RU, "Discord работает только через HTTP-прокси, а задан SOCKS5")
        },
        @SerialName("network")
        private val network: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Could not reach Discord: %error%")
            translation(MinecraftLocales.RU_RU, "Не удалось подключиться к Discord: %error%")
        },
        @SerialName("network_via_proxy")
        private val networkViaProxy: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Could not reach Discord through the proxy %proxy%: %error%")
            translation(MinecraftLocales.RU_RU, "Не удалось подключиться к Discord через прокси %proxy%: %error%")
        },
        @SerialName("unknown")
        private val unknown: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Discord error: %error%")
            translation(MinecraftLocales.RU_RU, "Ошибка Discord: %error%")
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
