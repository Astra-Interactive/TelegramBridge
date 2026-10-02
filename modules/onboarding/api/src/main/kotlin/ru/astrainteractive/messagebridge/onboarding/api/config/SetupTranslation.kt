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
            &#42f596MessageBridge:
            &#42f596/mb status &7— state of the bots
            &#42f596/mb reload &7— reload config.yml and the translation folder
            &#42f596/mb telegram &7— state of Telegram
            &#42f596/mb telegram token <token>
            &#42f596/mb telegram chat <chat_id>
            &#42f596/mb telegram topic <topic_id|none>
            &#42f596/mb telegram proxy <http|socks5> <host> <port> [username] [password]
            &#42f596/mb telegram proxy off
            &#42f596/mb telegram api-url <url|default>
            &#42f596/mb telegram bind &7— bind the chat with a one-time code
            &#42f596/mb discord &7— state of Discord
            &#42f596/mb discord token <token>
            &#42f596/mb discord channel <channel_id>
            &#42f596/mb discord activity <text>
            &#42f596/mb discord proxy http <host> <port> [username] [password]
            &#42f596/mb discord proxy off
            &#42f596/mb discord bind &7— bind the channel with a one-time code
            &#42f596/mb discord invite &7— invite link of the bot
            &#dbbb18In game, end a token or a password with --unsafe
            &7Docs: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/commands.md
            """.trimIndent()
        )
        translation(
            MinecraftLocales.RU_RU,
            """
            &#42f596MessageBridge:
            &#42f596/mb status &7— состояние ботов
            &#42f596/mb reload &7— перечитать config.yml и папку translation
            &#42f596/mb telegram &7— состояние Telegram
            &#42f596/mb telegram token <токен>
            &#42f596/mb telegram chat <chat_id>
            &#42f596/mb telegram topic <topic_id|none>
            &#42f596/mb telegram proxy <http|socks5> <хост> <порт> [логин] [пароль]
            &#42f596/mb telegram proxy off
            &#42f596/mb telegram api-url <url|default>
            &#42f596/mb telegram bind &7— привязать чат одноразовым кодом
            &#42f596/mb discord &7— состояние Discord
            &#42f596/mb discord token <токен>
            &#42f596/mb discord channel <channel_id>
            &#42f596/mb discord activity <текст>
            &#42f596/mb discord proxy http <хост> <порт> [логин] [пароль]
            &#42f596/mb discord proxy off
            &#42f596/mb discord bind &7— привязать канал одноразовым кодом
            &#42f596/mb discord invite &7— ссылка-приглашение бота
            &#dbbb18В игре добавляйте --unsafe после токена или пароля
            &7Документация: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/commands.md
            """.trimIndent()
        )
    },
    @SerialName("unsafe_required")
    val unsafeRequired: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#db2c18Not saved: game commands go to the server log. Use the console or add --unsafe"
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#db2c18Не сохранено: команды из игры пишутся в лог сервера. Используйте консоль или добавьте --unsafe"
        )
    },
    @SerialName("config_broken")
    private val configBroken: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18config.yml has an error, nothing is saved:\n%error%")
        translation(MinecraftLocales.RU_RU, "&#db2c18В config.yml ошибка, ничего не сохранено:\n%error%")
    },
    @SerialName("reload_config_error")
    private val reloadConfigError: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18config.yml has an error, the previous settings are kept:\n%error%")
        translation(MinecraftLocales.RU_RU, "&#db2c18В config.yml ошибка, работают прежние настройки:\n%error%")
    },
    @SerialName("invalid_telegram_token")
    val invalidTelegramToken: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18Invalid Telegram bot token")
        translation(MinecraftLocales.RU_RU, "&#db2c18Неверный токен Telegram-бота")
    },
    @SerialName("invalid_discord_token")
    val invalidDiscordToken: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18Invalid Discord bot token")
        translation(MinecraftLocales.RU_RU, "&#db2c18Неверный токен Discord-бота")
    },
    @SerialName("invalid_chat")
    val invalidChat: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18chat_id is a non-zero number")
        translation(MinecraftLocales.RU_RU, "&#db2c18chat_id — ненулевое число")
    },
    @SerialName("invalid_topic")
    val invalidTopic: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18topic_id is a positive number or none")
        translation(MinecraftLocales.RU_RU, "&#db2c18topic_id — положительное число или none")
    },
    @SerialName("invalid_channel")
    val invalidChannel: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18channel_id is a number of 17 to 20 digits")
        translation(MinecraftLocales.RU_RU, "&#db2c18channel_id — число из 17–20 цифр")
    },
    @SerialName("invalid_host")
    val invalidHost: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18Invalid proxy host")
        translation(MinecraftLocales.RU_RU, "&#db2c18Неверный хост прокси")
    },
    @SerialName("invalid_port")
    val invalidPort: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18The port is a number from 1 to 65535")
        translation(MinecraftLocales.RU_RU, "&#db2c18Порт — число от 1 до 65535")
    },
    @SerialName("invalid_proxy_type")
    val invalidProxyType: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18The proxy type is http or socks5")
        translation(MinecraftLocales.RU_RU, "&#db2c18Тип прокси — http или socks5")
    },
    @SerialName("invalid_proxy_credentials")
    val invalidProxyCredentials: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18Only a username and a password can follow the port")
        translation(MinecraftLocales.RU_RU, "&#db2c18После порта указываются только логин и пароль")
    },
    @SerialName("invalid_url")
    val invalidUrl: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18Invalid Bot API address: a server without a path, or default")
        translation(MinecraftLocales.RU_RU, "&#db2c18Неверный адрес Bot API: нужен сервер без пути или default")
    },
    @SerialName("socks_not_supported")
    val socksNotSupported: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#db2c18Discord works only through an HTTP proxy")
        translation(MinecraftLocales.RU_RU, "&#db2c18Discord работает только через HTTP-прокси")
    },
    @SerialName("telegram_bind_issued")
    private val telegramBindIssued: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#42f596Send %command% to the Telegram chat or topic. The code is valid for %minutes% min"
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#42f596Отправьте %command% в чат или топик Telegram. Код действует %minutes% мин"
        )
    },
    @SerialName("discord_bind_issued")
    private val discordBindIssued: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            "&#42f596Send !bind %code% to the Discord channel. The code is valid for %minutes% min"
        )
        translation(
            MinecraftLocales.RU_RU,
            "&#42f596Отправьте !bind %code% в канал Discord. Код действует %minutes% мин"
        )
    },
    @SerialName("invite_link")
    private val inviteLink: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#42f596Invite link: %link%")
        translation(MinecraftLocales.RU_RU, "&#42f596Ссылка-приглашение: %link%")
    },
    @SerialName("invite_unavailable")
    val inviteUnavailable: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#dbbb18The Discord bot is not connected")
        translation(MinecraftLocales.RU_RU, "&#dbbb18Бот Discord не подключён")
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

    fun inviteLink(link: LocalizableComponent): LocalizableComponent = inviteLink.replace("%link%", link)

    @Serializable
    data class Saved(
        @SerialName("token")
        private val token: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Token %token% saved")
            translation(MinecraftLocales.RU_RU, "&#42f596Токен %token% сохранён")
        },
        @SerialName("chat")
        private val chat: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Chat %chat% saved")
            translation(MinecraftLocales.RU_RU, "&#42f596Чат %chat% сохранён")
        },
        @SerialName("topic")
        private val topic: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Topic %topic% saved")
            translation(MinecraftLocales.RU_RU, "&#42f596Топик %topic% сохранён")
        },
        @SerialName("topic_removed")
        val topicRemoved: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Topic removed")
            translation(MinecraftLocales.RU_RU, "&#42f596Топик убран")
        },
        @SerialName("channel")
        private val channel: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Channel %channel% saved")
            translation(MinecraftLocales.RU_RU, "&#42f596Канал %channel% сохранён")
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
            translation(MinecraftLocales.EN_US, "&#42f596Bot API address reset to api.telegram.org")
            translation(MinecraftLocales.RU_RU, "&#42f596Адрес Bot API сброшен на api.telegram.org")
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
            translation(MinecraftLocales.EN_US, "&#dbbb18%messenger%: no token")
            translation(MinecraftLocales.RU_RU, "&#dbbb18%messenger%: нет токена")
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
            translation(MinecraftLocales.EN_US, "&#dbbb18  Chat: not set")
            translation(MinecraftLocales.RU_RU, "&#dbbb18  Чат: не задан")
        },
        @SerialName("channel")
        private val channel: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&7  Channel: %channel%")
            translation(MinecraftLocales.RU_RU, "&7  Канал: %channel%")
        },
        @SerialName("no_channel")
        val noChannel: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#dbbb18  Channel: not set")
            translation(MinecraftLocales.RU_RU, "&#dbbb18  Канал: не задан")
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
            translation(MinecraftLocales.EN_US, "&#db2c18  Last delivery error: %reason%")
            translation(MinecraftLocales.RU_RU, "&#db2c18  Последняя ошибка доставки: %reason%")
        }
    ) {
        fun disabled(messenger: String): LocalizableComponent = disabled.replace("%messenger%", messenger)

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
