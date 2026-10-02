package ru.astrainteractive.messagebridge.core.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import kotlin.time.Duration

@Serializable
data class TelegramTranslation(
    @SerialName("anonymous_author")
    val anonymousAuthor: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "Anonymous")
        translation(MinecraftLocales.RU_RU, "Аноним")
    },
    @SerialName("status")
    val status: Status = Status(),
    @SerialName("errors")
    val errors: Errors = Errors()
) {
    @Serializable
    data class Status(
        @SerialName("connected")
        private val connected: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Telegram bot %bot% is connected")
            translation(MinecraftLocales.RU_RU, "Бот Telegram %bot% подключён")
        },
        @SerialName("delivery_failed")
        private val deliveryFailed: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Could not send a message to Telegram: %reason%")
            translation(MinecraftLocales.RU_RU, "Не удалось отправить сообщение в Telegram: %reason%")
        },
        @SerialName("delivery_restored")
        val deliveryRestored: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Messages are delivered to Telegram again")
            translation(MinecraftLocales.RU_RU, "Сообщения снова доставляются в Telegram")
        }
    ) {
        fun connected(botName: String): LocalizableComponent = connected.replaceAll(
            PlaceholderReplacement.plain("%bot%", botName)
        )

        fun deliveryFailed(reason: LocalizableComponent): LocalizableComponent = deliveryFailed.replaceAll(
            PlaceholderReplacement(placeholder = "%reason%", value = reason)
        )
    }

    @Serializable
    data class Errors(
        @SerialName("invalid_token")
        val invalidToken: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The bot token is invalid or revoked")
            translation(MinecraftLocales.RU_RU, "Токен бота неверный или отозван")
        },
        @SerialName("chat_not_set")
        val chatNotSet: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "chat_id is not set")
            translation(MinecraftLocales.RU_RU, "chat_id не задан")
        },
        @SerialName("chat_not_found")
        val chatNotFound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Chat not found: chat_id is wrong or the bot is not in the chat")
            translation(MinecraftLocales.RU_RU, "Чат не найден: chat_id неверный или бота нет в чате")
        },
        @SerialName("chat_migrated")
        private val chatMigrated: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The group became a supergroup with the new id %chat_id%")
            translation(MinecraftLocales.RU_RU, "Группа стала супергруппой с новым id %chat_id%")
        },
        @SerialName("chat_id_changed")
        private val chatIdChanged: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The group became a supergroup, chat_id is changed to %chat_id%")
            translation(MinecraftLocales.RU_RU, "Группа стала супергруппой, chat_id изменён на %chat_id%")
        },
        @SerialName("topic_not_found")
        val topicNotFound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The topic from topic_id is not found or closed")
            translation(MinecraftLocales.RU_RU, "Топик из topic_id не найден или закрыт")
        },
        @SerialName("bot_not_in_chat")
        val botNotInChat: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The bot is not in the chat")
            translation(MinecraftLocales.RU_RU, "Бота нет в чате")
        },
        @SerialName("no_rights")
        val noRights: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The bot has no rights for this in the chat")
            translation(MinecraftLocales.RU_RU, "У бота нет прав на это в чате")
        },
        @SerialName("token_in_use")
        val tokenInUse: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The token is used by another program (409 Conflict)")
            translation(MinecraftLocales.RU_RU, "Токен использует другая программа (409 Conflict)")
        },
        @SerialName("rate_limited")
        val rateLimited: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Telegram rate limit, sending continues in about a minute")
            translation(MinecraftLocales.RU_RU, "Ограничение Telegram, отправка продолжится примерно через минуту")
        },
        @SerialName("rate_limited_for")
        private val rateLimitedFor: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Telegram rate limit, sending continues in %seconds% s")
            translation(MinecraftLocales.RU_RU, "Ограничение Telegram, отправка продолжится через %seconds% с")
        },
        @SerialName("network")
        val network: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Can't connect to Telegram")
            translation(MinecraftLocales.RU_RU, "Не удаётся подключиться к Telegram")
        },
        @SerialName("network_proxy")
        private val networkProxy: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Can't connect to Telegram through the proxy %proxy%")
            translation(MinecraftLocales.RU_RU, "Не удаётся подключиться к Telegram через прокси %proxy%")
        },
        @SerialName("invalid_proxy")
        private val invalidProxy: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Invalid proxy address %proxy%")
            translation(MinecraftLocales.RU_RU, "Неверный адрес прокси %proxy%")
        },
        @SerialName("network_api_url")
        private val networkApiUrl: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Can't connect to the Bot API server %url%")
            translation(MinecraftLocales.RU_RU, "Не удаётся подключиться к серверу Bot API %url%")
        },
        @SerialName("proxy_auth")
        val proxyAuth: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The proxy rejected the username or password")
            translation(MinecraftLocales.RU_RU, "Прокси не принял логин или пароль")
        },
        @SerialName("invalid_api_url")
        val invalidApiUrl: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "api_url is not a Bot API server")
            translation(MinecraftLocales.RU_RU, "api_url — не сервер Bot API")
        },
        @SerialName("socks_with_password")
        val socksWithPassword: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "A SOCKS5 proxy with a password is not supported")
            translation(MinecraftLocales.RU_RU, "SOCKS5-прокси с паролем не поддерживается")
        },
        @SerialName("server_error")
        private val serverError: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Telegram server error %code%")
            translation(MinecraftLocales.RU_RU, "Ошибка сервера Telegram %code%")
        },
        @SerialName("unknown")
        private val unknown: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Telegram error: %error%")
            translation(MinecraftLocales.RU_RU, "Ошибка Telegram: %error%")
        }
    ) {
        fun chatMigrated(chatId: Long): LocalizableComponent = chatMigrated.replaceAll(
            PlaceholderReplacement.plain("%chat_id%", "$chatId")
        )

        fun chatIdChanged(chatId: Long): LocalizableComponent = chatIdChanged.replaceAll(
            PlaceholderReplacement.plain("%chat_id%", "$chatId")
        )

        fun rateLimitedFor(retryAfter: Duration): LocalizableComponent = rateLimitedFor.replaceAll(
            PlaceholderReplacement.plain("%seconds%", "${retryAfter.inWholeSeconds}")
        )

        fun networkProxy(proxy: String): LocalizableComponent = networkProxy.replaceAll(
            PlaceholderReplacement.plain("%proxy%", proxy)
        )

        fun invalidProxy(proxy: String): LocalizableComponent = invalidProxy.replaceAll(
            PlaceholderReplacement.plain("%proxy%", proxy)
        )

        fun networkApiUrl(url: String): LocalizableComponent = networkApiUrl.replaceAll(
            PlaceholderReplacement.plain("%url%", url)
        )

        fun serverError(code: Int): LocalizableComponent = serverError.replaceAll(
            PlaceholderReplacement.plain("%code%", "$code")
        )

        fun unknown(error: String): LocalizableComponent = unknown.replaceAll(
            PlaceholderReplacement.plain("%error%", error)
        )
    }
}
