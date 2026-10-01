package ru.astrainteractive.messagebridge.onboarding.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

@Serializable
data class TelegramSetupTranslation(
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
            Guide: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/telegram.md
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
            Инструкция (на английском): https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/telegram.md
            """.trimIndent()
        )
    },
    @SerialName("check")
    val check: Check = Check(),
    @SerialName("chat_info")
    val chatInfo: ChatInfo = ChatInfo(),
    @SerialName("bind")
    val bind: Bind = Bind()
) {
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
        },
        @SerialName("reconnecting")
        val reconnecting: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot is reconnecting with the new settings. Run the check again in a few seconds"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бот переподключается с новыми настройками. Повторите проверку через несколько секунд"
            )
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
        fun message(chatId: String, topicId: String, type: String, isForum: Boolean): LocalizableComponent {
            return message.replaceAll(
                PlaceholderReplacement.plain("%chat_id%", chatId),
                PlaceholderReplacement.plain("%topic_id%", topicId),
                PlaceholderReplacement.plain("%type%", type),
                PlaceholderReplacement(placeholder = "%forum%", value = if (isForum) forumOn else forumOff)
            )
        }
    }

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
        },
        @SerialName("save_failed")
        val saveFailed: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "❌ The server could not save this chat: its config.yml has an error. " +
                    "Ask the admin of the server to fix it"
            )
            translation(
                MinecraftLocales.RU_RU,
                "❌ Сервер не смог сохранить этот чат: в его config.yml ошибка. " +
                    "Попросите администратора сервера её исправить"
            )
        },
        @SerialName("not_bound")
        private val notBound: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                """Telegram chat "%chat%" is not connected: config.yml has an error (%error%). """ +
                    "Fix the file, run /mb reload and connect the chat again: /mb telegram bind"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Чат Telegram «%chat%» не подключён: в config.yml ошибка (%error%). " +
                    "Исправьте файл, выполните /mb reload и подключите чат заново: /mb telegram bind"
            )
        }
    ) {
        fun notBound(chat: String, error: String): LocalizableComponent = notBound.replaceAll(
            PlaceholderReplacement.plain("%chat%", chat),
            PlaceholderReplacement.plain("%error%", error)
        )

        fun bound(chat: String, topic: String?): LocalizableComponent {
            if (topic == null) return bound.replaceAll(PlaceholderReplacement.plain("%chat%", chat))
            return boundTopic.replaceAll(
                PlaceholderReplacement.plain("%chat%", chat),
                PlaceholderReplacement.plain("%topic%", topic)
            )
        }
    }
}
