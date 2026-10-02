package ru.astrainteractive.messagebridge.onboarding.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replace
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import kotlin.time.Duration

@Serializable
data class SetupTranslation(
    @SerialName("help")
    val help: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            """
            &#42f596MessageBridge commands:
            &#42f596/mb status &7— state of the bots
            &#42f596/mb reload &7— reload config.yml and the translation folder
            &#42f596/mb telegram &7— Telegram setup guide and state
            &#42f596/mb telegram token <token> &7— set the bot token
            &#42f596/mb telegram chat <chat_id> &7— set the chat, e.g. -1001234567890
            &#42f596/mb telegram topic <topic_id|none> &7— set or clear the topic
            &#42f596/mb telegram proxy <http|socks5> <host> <port> [username] [password] &7— set a proxy
            &#42f596/mb telegram proxy off &7— remove the proxy
            &#42f596/mb telegram api-url <url|default> &7— use a self-hosted Bot API or a mirror
            &#42f596/mb telegram bind &7— bind the chat with a one-time code
            &#42f596/mb telegram check &7— check the bot and send a test message
            &#42f596/mb discord &7— Discord setup guide and state
            &#42f596/mb discord token <token> &7— set the bot token
            &#42f596/mb discord channel <channel_id> &7— set the channel
            &#42f596/mb discord activity <text> &7— set the activity of the bot
            &#42f596/mb discord proxy http <host> <port> [username] [password] &7— set a proxy
            &#42f596/mb discord proxy off &7— remove the proxy
            &#42f596/mb discord bind &7— bind the channel with a one-time code
            &#42f596/mb discord invite &7— link that adds the bot to your server
            &#42f596/mb discord check &7— check the bot and send a test message
            &#dbbb18In game, add --unsafe after a token or a password: game commands are written to the server log.
            &7Every command: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/commands.md
            """.trimIndent()
        )
        translation(
            MinecraftLocales.RU_RU,
            """
            &#42f596Команды MessageBridge:
            &#42f596/mb status &7— состояние ботов
            &#42f596/mb reload &7— перечитать config.yml и папку translation
            &#42f596/mb telegram &7— инструкция и состояние Telegram
            &#42f596/mb telegram token <токен> &7— задать токен бота
            &#42f596/mb telegram chat <chat_id> &7— задать чат, например -1001234567890
            &#42f596/mb telegram topic <topic_id|none> &7— задать или убрать топик
            &#42f596/mb telegram proxy <http|socks5> <хост> <порт> [логин] [пароль] &7— задать прокси
            &#42f596/mb telegram proxy off &7— убрать прокси
            &#42f596/mb telegram api-url <url|default> &7— свой Bot API или зеркало
            &#42f596/mb telegram bind &7— привязать чат одноразовым кодом
            &#42f596/mb telegram check &7— проверить бота и отправить тестовое сообщение
            &#42f596/mb discord &7— инструкция и состояние Discord
            &#42f596/mb discord token <токен> &7— задать токен бота
            &#42f596/mb discord channel <channel_id> &7— задать канал
            &#42f596/mb discord activity <текст> &7— задать статус бота
            &#42f596/mb discord proxy http <хост> <порт> [логин] [пароль] &7— задать прокси
            &#42f596/mb discord proxy off &7— убрать прокси
            &#42f596/mb discord bind &7— привязать канал одноразовым кодом
            &#42f596/mb discord invite &7— ссылка, чтобы добавить бота на сервер
            &#42f596/mb discord check &7— проверить бота и отправить тестовое сообщение
            &#dbbb18В игре добавляйте --unsafe после токена или пароля: команды из игры записываются в лог сервера.
            &7Все команды (на английском): https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/commands.md
            """.trimIndent()
        )
    },
    @SerialName("unsafe_required")
    val unsafeRequired: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18Nothing was saved. Commands sent from the game are written to the server log (latest.log), " +
                "so this secret is already there as plain text. Run the command in the server console, or add " +
                "--unsafe at the end to save it anyway. If other people can read the logs, reset the token " +
                "(Telegram: @BotFather → /revoke, Discord: Bot → Reset Token)."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18Ничего не сохранено. Команды из игры записываются в лог сервера (latest.log), так что этот " +
                "секрет уже лежит там открытым текстом. Выполните команду в консоли сервера или добавьте в конец " +
                "--unsafe, чтобы всё равно сохранить. Если логи читают другие люди, сбросьте токен " +
                "(Telegram: @BotFather → /revoke, Discord: Bot → Reset Token)."
        )
    },
    @SerialName("config_broken")
    private val configBroken: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18config.yml has an error, so nothing is saved. Fix the file, run /mb reload and try again:\n%error%"
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18В config.yml ошибка, поэтому ничего не сохранено. Исправьте файл, выполните /mb reload " +
                "и повторите команду:\n%error%"
        )
    },
    @SerialName("reload_config_error")
    private val reloadConfigError: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18config.yml has an error and is not applied, the previous settings are kept:\n%error%"
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18В config.yml ошибка, файл не применён и работают прежние настройки:\n%error%"
        )
    },
    @SerialName("invalid_telegram_token")
    val invalidTelegramToken: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18This is not a Telegram bot token. It looks like 123456789:AAH… — copy it from @BotFather."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18Это не токен Telegram-бота. Он выглядит как 123456789:AAH… — скопируйте его у @BotFather."
        )
    },
    @SerialName("invalid_discord_token")
    val invalidDiscordToken: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18This is not a Discord bot token. It has three parts separated by dots: copy it in Bot → " +
                "Reset Token, not the Client Secret."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18Это не токен Discord-бота. Он состоит из трёх частей через точку: скопируйте его в Bot → " +
                "Reset Token, а не Client Secret."
        )
    },
    @SerialName("invalid_chat")
    val invalidChat: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18The chat id is a number, e.g. -1001234567890. The easiest way to set it is /mb telegram bind."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18ID чата — это число, например -1001234567890. Проще всего задать его через /mb telegram bind."
        )
    },
    @SerialName("invalid_topic")
    val invalidTopic: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18The topic id is a positive number, or none to write to the whole chat."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18ID топика — положительное число, или none, чтобы писать в общий чат."
        )
    },
    @SerialName("invalid_channel")
    val invalidChannel: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18The channel id is a number of 17 to 20 digits. Turn on Developer Mode in Discord and use " +
                "Copy Channel ID, or run /mb discord bind."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18ID канала — число из 17–20 цифр. Включите режим разработчика в Discord и нажмите " +
                "«Копировать ID канала» или выполните /mb discord bind."
        )
    },
    @SerialName("invalid_host")
    val invalidHost: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18The proxy host is a domain or an IP address, e.g. proxy.example.com or 1.2.3.4."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18Хост прокси — домен или IP-адрес, например proxy.example.com или 1.2.3.4."
        )
    },
    @SerialName("invalid_port")
    val invalidPort: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18The port is a number from 1 to 65535.")
        translation(MinecraftLocales.RU_RU, "&#db2c18Порт — число от 1 до 65535.")
    },
    @SerialName("invalid_proxy_type")
    val invalidProxyType: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18The proxy type is http or socks5.")
        translation(MinecraftLocales.RU_RU, "&#db2c18Тип прокси — http или socks5.")
    },
    @SerialName("invalid_proxy_credentials")
    val invalidProxyCredentials: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18Only the username and the password of the proxy can follow the port."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18После порта указываются только логин и пароль прокси."
        )
    },
    @SerialName("invalid_url")
    val invalidUrl: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18The address is a Bot API server or a mirror without a path, e.g. http://localhost:8081 " +
                "or tg.example.com (https:// when no scheme is given). Use default to go back to api.telegram.org."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18Адрес — сервер Bot API или зеркало без пути, например http://localhost:8081 " +
                "или tg.example.com (без схемы — https://). Чтобы вернуть api.telegram.org, укажите default."
        )
    },
    @SerialName("socks_not_supported")
    val socksNotSupported: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18Discord works only through an HTTP proxy: /mb discord proxy http <host> <port>"
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18Discord работает только через HTTP-прокси: /mb discord proxy http <хост> <порт>"
        )
    },
    @SerialName("telegram_bind_issued")
    private val telegramBindIssued: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#42f596Send %command% to the Telegram chat, inside the topic if you use topics. " +
                "The code is valid for %minutes% minutes."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#42f596Отправьте %command% в чат Telegram (в нужный топик, если они есть). " +
                "Код действует %minutes% мин."
        )
    },
    @SerialName("discord_bind_issued")
    private val discordBindIssued: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#42f596Send !bind %code% to the Discord channel. The code is valid for %minutes% minutes."
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#42f596Отправьте !bind %code% в нужный канал Discord. Код действует %minutes% мин."
        )
    },
    @SerialName("check_started")
    private val checkStarted: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#dbbb18Checking %messenger%…")
        translation(MinecraftLocales.RU_RU, "&#dbbb18Проверяю %messenger%…")
    },
    @SerialName("check_ok")
    private val checkOk: LocalizedText = LocalizedText.shared("&#42f596✔ %check%"),
    @SerialName("check_warning")
    private val checkWarning: LocalizedText = LocalizedText.shared("&#dbbb18⚠ %check%"),
    @SerialName("check_error")
    private val checkError: LocalizedText = LocalizedText.shared("&#db2c18✖ %check%"),
    @SerialName("invite_link")
    private val inviteLink: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#42f596Open the link to add the bot to your server: %link%")
        translation(MinecraftLocales.RU_RU, "&#42f596Откройте ссылку, чтобы добавить бота на сервер: %link%")
    },
    @SerialName("invite_unavailable")
    val inviteUnavailable: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#dbbb18The link is available once the bot is connected, see /mb discord")
        translation(MinecraftLocales.RU_RU, "&#dbbb18Ссылка появится, когда бот подключится, см. /mb discord")
    },
    @SerialName("saved")
    val saved: Saved = Saved(),
    @SerialName("status")
    val status: Status = Status()
) {
    fun configBroken(error: String): LocalizableComponent = configBroken.replace("%error%", error)

    fun reloadConfigError(error: String): LocalizableComponent = reloadConfigError.replace("%error%", error)

    fun telegramBindIssued(command: String, lifetime: Duration): LocalizableComponent = telegramBindIssued.replaceAll(
        PlaceholderReplacement.plain("%command%", command),
        PlaceholderReplacement.plain("%minutes%", "${lifetime.inWholeMinutes}")
    )

    fun discordBindIssued(code: String, lifetime: Duration): LocalizableComponent = discordBindIssued.replaceAll(
        PlaceholderReplacement.plain("%code%", code),
        PlaceholderReplacement.plain("%minutes%", "${lifetime.inWholeMinutes}")
    )

    fun checkStarted(messenger: String): LocalizableComponent = checkStarted.replace("%messenger%", messenger)

    fun checkOk(check: LocalizableComponent): LocalizableComponent = checkOk.replace("%check%", check)

    fun checkWarning(check: LocalizableComponent): LocalizableComponent = checkWarning.replace("%check%", check)

    fun checkError(check: LocalizableComponent): LocalizableComponent = checkError.replace("%check%", check)

    fun inviteLink(link: LocalizableComponent): LocalizableComponent = inviteLink.replace("%link%", link)

    @Serializable
    data class Saved(
        @SerialName("token")
        private val token: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Token %token% saved, connecting…")
            translation(MinecraftLocales.RU_RU, "&#42f596Токен %token% сохранён, подключаюсь…")
        },
        @SerialName("chat")
        private val chat: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "&#42f596Chat %chat% saved. Send a test message with /mb telegram check"
            )
            translation(
                MinecraftLocales.RU_RU,
                "&#42f596Чат %chat% сохранён. Отправьте тестовое сообщение: /mb telegram check"
            )
        },
        @SerialName("topic")
        private val topic: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Topic %topic% saved")
            translation(MinecraftLocales.RU_RU, "&#42f596Топик %topic% сохранён")
        },
        @SerialName("topic_removed")
        val topicRemoved: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Topic removed, messages go to the whole chat")
            translation(MinecraftLocales.RU_RU, "&#42f596Топик убран, сообщения идут в общий чат")
        },
        @SerialName("channel")
        private val channel: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "&#42f596Channel %channel% saved. Send a test message with /mb discord check"
            )
            translation(
                MinecraftLocales.RU_RU,
                "&#42f596Канал %channel% сохранён. Отправьте тестовое сообщение: /mb discord check"
            )
        },
        @SerialName("activity")
        private val activity: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Activity saved: %activity%")
            translation(MinecraftLocales.RU_RU, "&#42f596Статус бота сохранён: %activity%")
        },
        @SerialName("proxy")
        private val proxy: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Proxy %proxy% saved")
            translation(MinecraftLocales.RU_RU, "&#42f596Прокси %proxy% сохранён")
        },
        @SerialName("proxy_removed")
        val proxyRemoved: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Proxy removed")
            translation(MinecraftLocales.RU_RU, "&#42f596Прокси убран")
        },
        @SerialName("api_url")
        private val apiUrl: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Bot API address %url% saved")
            translation(MinecraftLocales.RU_RU, "&#42f596Адрес Bot API %url% сохранён")
        },
        @SerialName("api_url_removed")
        val apiUrlRemoved: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596The bot uses api.telegram.org again")
            translation(MinecraftLocales.RU_RU, "&#42f596Бот снова работает через api.telegram.org")
        }
    ) {
        fun token(maskedToken: String): LocalizableComponent = token.replace("%token%", maskedToken)

        fun chat(chatId: String): LocalizableComponent = chat.replace("%chat%", chatId)

        fun topic(topicId: String): LocalizableComponent = topic.replace("%topic%", topicId)

        fun channel(channelId: String): LocalizableComponent = channel.replace("%channel%", channelId)

        fun activity(activity: String): LocalizableComponent = this.activity.replace("%activity%", activity)

        fun proxy(proxy: String): LocalizableComponent = this.proxy.replace("%proxy%", proxy)

        fun apiUrl(url: String): LocalizableComponent = apiUrl.replace("%url%", url)
    }

    @Serializable
    data class Status(
        @SerialName("header")
        val header: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596MessageBridge status:")
            translation(MinecraftLocales.RU_RU, "&#42f596Состояние MessageBridge:")
        },
        @SerialName("disabled")
        private val disabled: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#dbbb18%messenger%: not configured — /mb %command% token <token>")
            translation(MinecraftLocales.RU_RU, "&#dbbb18%messenger%: не настроен — /mb %command% token <токен>")
        },
        @SerialName("connecting")
        private val connecting: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#dbbb18%messenger%: connecting…")
            translation(MinecraftLocales.RU_RU, "&#dbbb18%messenger%: подключается…")
        },
        @SerialName("connected")
        private val connected: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596%messenger%: connected as %bot%")
            translation(MinecraftLocales.RU_RU, "&#42f596%messenger%: подключён как %bot%")
        },
        @SerialName("failed")
        private val failed: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#db2c18%messenger%: not connected — %reason%")
            translation(MinecraftLocales.RU_RU, "&#db2c18%messenger%: не подключён — %reason%")
        },
        @SerialName("chat")
        private val chat: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&7  Chat: %chat%")
            translation(MinecraftLocales.RU_RU, "&7  Чат: %chat%")
        },
        @SerialName("chat_with_topic")
        private val chatWithTopic: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&7  Chat: %chat%, topic %topic%")
            translation(MinecraftLocales.RU_RU, "&7  Чат: %chat%, топик %topic%")
        },
        @SerialName("no_chat")
        val noChat: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#dbbb18  Chat is not set — /mb telegram bind")
            translation(MinecraftLocales.RU_RU, "&#dbbb18  Чат не задан — /mb telegram bind")
        },
        @SerialName("channel")
        private val channel: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&7  Channel: %channel%")
            translation(MinecraftLocales.RU_RU, "&7  Канал: %channel%")
        },
        @SerialName("no_channel")
        val noChannel: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#dbbb18  Channel is not set — /mb discord bind")
            translation(MinecraftLocales.RU_RU, "&#dbbb18  Канал не задан — /mb discord bind")
        },
        @SerialName("proxy")
        private val proxy: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&7  Proxy: %proxy%")
            translation(MinecraftLocales.RU_RU, "&7  Прокси: %proxy%")
        },
        @SerialName("no_proxy")
        val noProxy: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&7  Proxy: none")
            translation(MinecraftLocales.RU_RU, "&7  Прокси: нет")
        },
        @SerialName("api_url")
        private val apiUrl: LocalizedText = LocalizedText.shared("&7  Bot API: %url%"),
        @SerialName("delivery_error")
        private val deliveryError: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#db2c18  The last message was not delivered: %reason%")
            translation(MinecraftLocales.RU_RU, "&#db2c18  Последнее сообщение не доставлено: %reason%")
        }
    ) {
        fun disabled(messenger: String, command: String): LocalizableComponent = disabled.replaceAll(
            PlaceholderReplacement.plain("%messenger%", messenger),
            PlaceholderReplacement.plain("%command%", command)
        )

        fun connecting(messenger: String): LocalizableComponent = connecting.replace("%messenger%", messenger)

        fun connected(messenger: String, botName: String): LocalizableComponent = connected.replaceAll(
            PlaceholderReplacement.plain("%messenger%", messenger),
            PlaceholderReplacement.plain("%bot%", botName)
        )

        fun failed(messenger: String, reason: LocalizableComponent): LocalizableComponent = failed.replaceAll(
            PlaceholderReplacement.plain("%messenger%", messenger),
            PlaceholderReplacement("%reason%", reason)
        )

        fun chat(chatId: String): LocalizableComponent = chat.replace("%chat%", chatId)

        fun chatWithTopic(chatId: String, topicId: String): LocalizableComponent = chatWithTopic.replaceAll(
            PlaceholderReplacement.plain("%chat%", chatId),
            PlaceholderReplacement.plain("%topic%", topicId)
        )

        fun channel(channelId: String): LocalizableComponent = channel.replace("%channel%", channelId)

        fun proxy(proxy: String): LocalizableComponent = this.proxy.replace("%proxy%", proxy)

        fun apiUrl(url: String): LocalizableComponent = apiUrl.replace("%url%", url)

        fun deliveryError(reason: LocalizableComponent): LocalizableComponent =
            deliveryError.replace("%reason%", reason)
    }
}
