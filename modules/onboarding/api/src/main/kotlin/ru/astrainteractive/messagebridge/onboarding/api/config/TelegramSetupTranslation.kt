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
    @SerialName("chat_info")
    val chatInfo: ChatInfo = ChatInfo(),
    @SerialName("bind")
    val bind: Bind = Bind()
) {
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
