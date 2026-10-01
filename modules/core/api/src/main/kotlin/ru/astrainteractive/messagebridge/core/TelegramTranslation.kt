package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

/** Texts about setting up the Telegram bot and the errors it runs into. */
@Serializable
data class TelegramTranslation(
    @SerialName("guide")
    val guide: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            """
            Telegram is not configured yet:
             1. Open @BotFather in Telegram, send /newbot and copy the token it gives
             2. In the server console run: mb telegram token <token>
             3. Add the bot to your group and make it an admin
             4. Run mb telegram bind and send the code it shows into the group (inside the topic if you use topics)
             5. Check everything with mb telegram check
            Guide: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/en/telegram.md
            """.trimIndent()
        )
        translation(
            MinecraftLocales.RU_RU,
            """
            Telegram ещё не настроен:
             1. Откройте @BotFather в Telegram, отправьте /newbot и скопируйте токен
             2. В консоли сервера выполните: mb telegram token <токен>
             3. Добавьте бота в группу и сделайте его администратором
             4. Выполните mb telegram bind и отправьте код в группу (в нужный топик, если они есть)
             5. Проверьте настройку: mb telegram check
            Инструкция: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/ru/telegram.md
            """.trimIndent()
        )
    },
    @SerialName("errors")
    val errors: Errors = Errors(),
    @SerialName("check")
    val check: Check = Check(),
    @SerialName("chat_info")
    val chatInfo: ChatInfo = ChatInfo(),
    @SerialName("bind")
    val bind: Bind = Bind()
) {
    /** What went wrong with the bot and what to do about it, for the console and the game. */
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
                    "set a proxy: /mb telegram proxy http <host> <port>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удаётся подключиться к Telegram. Если Telegram заблокирован там, где стоит сервер, " +
                    "укажите прокси: /mb telegram proxy http <хост> <порт>"
            )
        },
        @SerialName("network_proxy")
        private val networkProxy: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Can't connect to Telegram through the proxy %proxy%. Check that the proxy works and set the " +
                    "right one: /mb telegram proxy <http|socks5> <host> <port>, or turn it off: /mb telegram proxy off"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удаётся подключиться к Telegram через прокси %proxy%. Проверьте, что прокси работает, и " +
                    "укажите верный: /mb telegram proxy <http|socks5> <хост> <порт> или отключите его: " +
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
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/en/troubleshooting.md"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Ошибка Telegram: %error%. Подробнее: " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/ru/troubleshooting.md"
            )
        }
    ) {
        fun chatMigrated(chatId: Long): LocalizableComponent = chatMigrated.replaceAll(
            PlaceholderReplacement.plain("%chat_id%", "$chatId")
        )

        fun chatIdChanged(chatId: Long): LocalizableComponent = chatIdChanged.replaceAll(
            PlaceholderReplacement.plain("%chat_id%", "$chatId")
        )

        fun rateLimitedFor(seconds: Int): LocalizableComponent = rateLimitedFor.replaceAll(
            PlaceholderReplacement.plain("%seconds%", "$seconds")
        )

        /** @param proxy address of the proxy as `host:port` */
        fun networkProxy(proxy: String): LocalizableComponent = networkProxy.replaceAll(
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

    /** Lines of `/mb telegram check`, for the console and the game. */
    @Serializable
    data class Check(
        @SerialName("token_missing")
        val tokenMissing: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The token is not set. Create a bot in @BotFather (/newbot) and set its token: " +
                    "/mb telegram token <token>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Токен не указан. Создайте бота в @BotFather (/newbot) и укажите его токен: " +
                    "/mb telegram token <токен>"
            )
        },
        @SerialName("bot_works")
        private val botWorks: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Bot %bot% works")
            translation(MinecraftLocales.RU_RU, "Бот %bot% работает")
        },
        @SerialName("chat_found")
        private val chatFound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, """Chat "%chat%" is found (%type%)""")
            translation(MinecraftLocales.RU_RU, "Чат «%chat%» найден (%type%)")
        },
        @SerialName("forum_found")
        private val forumFound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, """Chat "%chat%" is found (%type% with topics)""")
            translation(MinecraftLocales.RU_RU, "Чат «%chat%» найден (%type% с топиками)")
        },
        @SerialName("bot_admin")
        val botAdmin: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The bot is an admin of the chat")
            translation(MinecraftLocales.RU_RU, "Бот — администратор чата")
        },
        @SerialName("bot_cant_delete")
        val botCantDelete: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot can't delete messages, so too long messages and names rejected by display_name_regex " +
                    "stay in the chat. Give the bot the right to delete messages"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бот не может удалять сообщения, поэтому слишком длинные сообщения и имена, не прошедшие " +
                    "display_name_regex, остаются в чате. Дайте боту право удалять сообщения"
            )
        },
        @SerialName("bot_not_admin")
        val botNotAdmin: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot is not an admin, so it can't delete too long messages and names rejected by " +
                    "display_name_regex. Make the bot an admin of the group"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бот не администратор, поэтому не может удалять слишком длинные сообщения и имена, не прошедшие " +
                    "display_name_regex. Сделайте бота администратором группы"
            )
        },
        @SerialName("privacy_mode")
        val privacyMode: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Privacy mode is on and the bot is not an admin, so it sees only commands, not the chat. " +
                    "Make the bot an admin, or turn privacy off in @BotFather: /mybots → your bot → " +
                    "Bot Settings → Group Privacy → Turn off, then remove the bot from the group and add it again"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Включён режим приватности, а бот не администратор, поэтому он видит только команды, а не " +
                    "переписку. Сделайте бота администратором или отключите приватность в @BotFather: /mybots → " +
                    "ваш бот → Bot Settings → Group Privacy → Turn off, затем удалите бота из группы и добавьте снова"
            )
        },
        @SerialName("forum_without_topic")
        val forumWithoutTopic: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The chat has topics, but topic_id is empty, so messages go to General. To use another topic, " +
                    "run /mb telegram bind and send the code inside that topic"
            )
            translation(
                MinecraftLocales.RU_RU,
                "В чате есть топики, но topic_id пустой, поэтому сообщения идут в General. Чтобы использовать " +
                    "другой топик, выполните /mb telegram bind и отправьте код внутри него"
            )
        },
        @SerialName("reply_thread")
        private val replyThread: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The chat has no topics, so topic_id %topic_id% makes a reply thread: only replies to message " +
                    "%topic_id% are relayed. To relay the whole chat: /mb telegram topic none"
            )
            translation(
                MinecraftLocales.RU_RU,
                "В чате нет топиков, поэтому topic_id %topic_id% работает как ветка ответов: пересылаются только " +
                    "ответы на сообщение %topic_id%. Чтобы пересылать весь чат: /mb telegram topic none"
            )
        },
        @SerialName("test_message")
        val testMessage: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "✅ MessageBridge check: the server can send messages to this chat")
            translation(
                MinecraftLocales.RU_RU,
                "✅ Проверка MessageBridge: сервер может отправлять сообщения в этот чат"
            )
        },
        @SerialName("test_message_sent")
        val testMessageSent: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The test message is sent to the chat")
            translation(MinecraftLocales.RU_RU, "Тестовое сообщение отправлено в чат")
        }
    ) {
        fun botWorks(botName: String): LocalizableComponent = botWorks.replaceAll(
            PlaceholderReplacement.plain("%bot%", botName)
        )

        fun chatFound(title: String, type: String, isForum: Boolean): LocalizableComponent {
            val text = if (isForum) forumFound else chatFound
            return text.replaceAll(
                PlaceholderReplacement.plain("%chat%", title),
                PlaceholderReplacement.plain("%type%", type)
            )
        }

        fun replyThread(topicId: String): LocalizableComponent = replyThread.replaceAll(
            PlaceholderReplacement.plain("%topic_id%", topicId)
        )
    }

    /** Reply to `/minfo`, which works in any chat so its ids can be found before it is set up. */
    @Serializable
    data class ChatInfo(
        @SerialName("message")
        private val message: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                """
                chat_id: %chat_id%
                topic_id: %topic_id%
                Chat type: %type%
                Topics: %forum%

                To connect this chat, run /mb telegram bind on the server and send the code here
                """.trimIndent()
            )
            translation(
                MinecraftLocales.RU_RU,
                """
                chat_id: %chat_id%
                topic_id: %topic_id%
                Тип чата: %type%
                Топики: %forum%

                Чтобы подключить этот чат, выполните /mb telegram bind на сервере и отправьте код сюда
                """.trimIndent()
            )
        },
        @SerialName("forum_on")
        private val forumOn: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "on")
            translation(MinecraftLocales.RU_RU, "включены")
        },
        @SerialName("forum_off")
        private val forumOff: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "off")
            translation(MinecraftLocales.RU_RU, "выключены")
        }
    ) {
        /** @param topicId id of the topic, or `none` outside of a topic */
        fun message(chatId: String, topicId: String, type: String, isForum: Boolean): LocalizableComponent {
            return message.replaceAll(
                PlaceholderReplacement.plain("%chat_id%", chatId),
                PlaceholderReplacement.plain("%topic_id%", topicId),
                PlaceholderReplacement.plain("%type%", type),
                PlaceholderReplacement(placeholder = "%forum%", value = if (isForum) forumOn else forumOff)
            )
        }
    }

    /** Replies to `/bind <code>` in Telegram and the text for the one who asked for the code. */
    @Serializable
    data class Bind(
        @SerialName("invalid_code")
        val invalidCode: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The code is wrong or expired. Run /mb telegram bind on the server to get a new one"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Код неверный или устарел. Выполните /mb telegram bind на сервере, чтобы получить новый"
            )
        },
        @SerialName("admins_only")
        val adminsOnly: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Only admins of the group can connect it")
            translation(MinecraftLocales.RU_RU, "Подключить группу могут только её администраторы")
        },
        @SerialName("success")
        val success: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "✅ This chat is connected: messages from the Minecraft server will come here"
            )
            translation(
                MinecraftLocales.RU_RU,
                "✅ Чат подключён: сюда будут приходить сообщения с сервера Minecraft"
            )
        },
        @SerialName("bound")
        private val bound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, """Telegram chat "%chat%" is connected""")
            translation(MinecraftLocales.RU_RU, "Чат Telegram «%chat%» подключён")
        },
        @SerialName("bound_topic")
        private val boundTopic: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, """Telegram chat "%chat%", topic "%topic%" is connected""")
            translation(MinecraftLocales.RU_RU, "Чат Telegram «%chat%», топик «%topic%» подключён")
        }
    ) {
        /** @param topic name or id of the topic, `null` when the chat is bound without a topic */
        fun bound(chat: String, topic: String?): LocalizableComponent {
            if (topic == null) return bound.replaceAll(PlaceholderReplacement.plain("%chat%", chat))
            return boundTopic.replaceAll(
                PlaceholderReplacement.plain("%chat%", chat),
                PlaceholderReplacement.plain("%topic%", topic)
            )
        }
    }
}
