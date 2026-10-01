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
            translation(
                MinecraftLocales.EN_US,
                "The bot token is invalid or was revoked. Copy the token from @BotFather " +
                    "(/mybots → your bot → API Token) and set it: /mb telegram token <token>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Токен бота неверный или был отозван. Скопируйте токен в @BotFather " +
                    "(/mybots → ваш бот → API Token) и укажите его: /mb telegram token <токен>"
            )
        },
        @SerialName("chat_not_set")
        val chatNotSet: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The chat is not set, so messages have nowhere to go. " +
                    "Run /mb telegram bind and send the code into your group"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Чат не указан, и сообщениям некуда уходить. " +
                    "Выполните /mb telegram bind и отправьте код в свою группу"
            )
        },
        @SerialName("chat_not_found")
        val chatNotFound: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Chat not found: chat_id is wrong or the bot is not in that chat. " +
                    "Add the bot to the group and run /mb telegram bind"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Чат не найден: chat_id указан неверно или бота нет в этом чате. " +
                    "Добавьте бота в группу и выполните /mb telegram bind"
            )
        },
        @SerialName("chat_migrated")
        private val chatMigrated: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The group was upgraded to a supergroup and got a new id %chat_id%. Set it: /mb telegram chat %chat_id%"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Группа стала супергруппой и получила новый id %chat_id%. Укажите его: /mb telegram chat %chat_id%"
            )
        },
        @SerialName("chat_id_changed")
        private val chatIdChanged: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The group was upgraded to a supergroup, so chat_id is changed to %chat_id% in config.yml"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Группа стала супергруппой, поэтому chat_id в config.yml изменён на %chat_id%"
            )
        },
        @SerialName("topic_not_found")
        val topicNotFound: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The topic from topic_id is not found or is closed. Run /mb telegram bind and send the code " +
                    "inside the right topic, or send to the main chat: /mb telegram topic none"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Топик из topic_id не найден или закрыт. Выполните /mb telegram bind и отправьте код " +
                    "в нужный топик или отправляйте в общий чат: /mb telegram topic none"
            )
        },
        @SerialName("bot_not_in_chat")
        val botNotInChat: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot is not in the chat: it was removed or never added. " +
                    "Add the bot to the group, make it an admin and check: /mb telegram check"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бота нет в чате: его удалили или ещё не добавили. " +
                    "Добавьте бота в группу, сделайте администратором и проверьте: /mb telegram check"
            )
        },
        @SerialName("no_rights")
        val noRights: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot has no rights for this in the chat. " +
                    "Make the bot an admin of the group with the rights to send and delete messages"
            )
            translation(
                MinecraftLocales.RU_RU,
                "У бота нет прав на это в чате. " +
                    "Сделайте бота администратором группы с правом отправлять и удалять сообщения"
            )
        },
        @SerialName("token_in_use")
        val tokenInUse: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "This token is used by another program (another server or a copy of the bot), and Telegram " +
                    "gives messages to only one of them. Stop the other one, or revoke the token in @BotFather " +
                    "(/mybots → your bot → API Token → Revoke) and set the new one: /mb telegram token <token>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Этот токен использует другая программа (другой сервер или копия бота), а Telegram отдаёт " +
                    "сообщения только одной из них. Остановите её или перевыпустите токен в @BotFather " +
                    "(/mybots → ваш бот → API Token → Revoke) и укажите новый: /mb telegram token <токен>"
            )
        },
        @SerialName("rate_limited")
        val rateLimited: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Telegram limits the bot for sending too many messages. Sending continues in a minute or so"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Telegram ограничил бота за слишком частые сообщения. Отправка возобновится примерно через минуту"
            )
        },
        @SerialName("rate_limited_for")
        private val rateLimitedFor: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Telegram limits the bot for sending too many messages. Sending continues in %seconds% s"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Telegram ограничил бота за слишком частые сообщения. Отправка возобновится через %seconds% с"
            )
        },
        @SerialName("network")
        val network: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Can't connect to Telegram. If Telegram is blocked where the server is, " +
                    "set a proxy: /mb telegram proxy http <host> <port>. " +
                    "Guide: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/proxy.md"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удаётся подключиться к Telegram. Если Telegram заблокирован там, где стоит сервер, " +
                    "укажите прокси: /mb telegram proxy http <хост> <порт>. Инструкция (на английском): " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/proxy.md"
            )
        },
        @SerialName("network_proxy")
        private val networkProxy: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Can't connect to Telegram through the proxy %proxy%. Check that the proxy works and set the " +
                    "right one: /mb telegram proxy <http|socks5> <host> <port>, or turn it off: " +
                    "/mb telegram proxy off. " +
                    "Guide: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/proxy.md"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удаётся подключиться к Telegram через прокси %proxy%. Проверьте, что прокси работает, и " +
                    "укажите верный: /mb telegram proxy <http|socks5> <хост> <порт> или отключите его: " +
                    "/mb telegram proxy off. Инструкция (на английском): " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/proxy.md"
            )
        },
        @SerialName("invalid_proxy")
        private val invalidProxy: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The proxy address %proxy% is invalid: it needs a host and a port from 1 to 65535. " +
                    "Set the right one: /mb telegram proxy <http|socks5> <host> <port>, or turn it off: " +
                    "/mb telegram proxy off"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Адрес прокси %proxy% неверный: нужны хост и порт от 1 до 65535. " +
                    "Укажите верный: /mb telegram proxy <http|socks5> <хост> <порт> или отключите прокси: " +
                    "/mb telegram proxy off"
            )
        },
        @SerialName("network_api_url")
        private val networkApiUrl: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Can't connect to the Bot API server %url%. Check the address: /mb telegram api-url <url>, " +
                    "or go back to Telegram: /mb telegram api-url default"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удаётся подключиться к серверу Bot API %url%. Проверьте адрес: /mb telegram api-url <url> " +
                    "или вернитесь к Telegram: /mb telegram api-url default"
            )
        },
        @SerialName("proxy_auth")
        val proxyAuth: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The proxy rejected the username or password. " +
                    "Set them again: /mb telegram proxy http <host> <port> <username> <password>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Прокси не принял логин или пароль. " +
                    "Укажите их заново: /mb telegram proxy http <хост> <порт> <логин> <пароль>"
            )
        },
        @SerialName("invalid_api_url")
        val invalidApiUrl: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "api_url is not a Bot API server. Set an address without a path, like https://api.example.com: " +
                    "/mb telegram api-url <url>, or go back to Telegram: /mb telegram api-url default"
            )
            translation(
                MinecraftLocales.RU_RU,
                "api_url не похож на сервер Bot API. Укажите адрес без пути, например https://api.example.com: " +
                    "/mb telegram api-url <url>, или вернитесь к Telegram: /mb telegram api-url default"
            )
        },
        @SerialName("socks_with_password")
        val socksWithPassword: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "A SOCKS5 proxy with a password is not supported. Use an HTTP proxy (it can have a password) " +
                    "or a SOCKS5 proxy without a password: /mb telegram proxy <http|socks5> <host> <port> " +
                    "[username] [password]"
            )
            translation(
                MinecraftLocales.RU_RU,
                "SOCKS5-прокси с паролем не поддерживается. Используйте HTTP-прокси (у него пароль может быть) " +
                    "или SOCKS5 без пароля: /mb telegram proxy <http|socks5> <хост> <порт> [логин] [пароль]"
            )
        },
        @SerialName("server_error")
        private val serverError: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Telegram answered with server error %code%. It usually passes by itself, the bot keeps trying"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Telegram ответил ошибкой сервера %code%. Обычно это проходит само, бот продолжает попытки"
            )
        },
        @SerialName("unknown")
        private val unknown: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Telegram error: %error%. See " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/troubleshooting.md"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Ошибка Telegram: %error%. Подробнее (на английском): " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/troubleshooting.md"
            )
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
