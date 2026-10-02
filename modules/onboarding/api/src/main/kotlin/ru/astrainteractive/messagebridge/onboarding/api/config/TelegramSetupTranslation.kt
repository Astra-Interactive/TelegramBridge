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
                """.trimIndent()
            )
            translation(
                MinecraftLocales.RU_RU,
                """
                chat_id: %chat_id%
                topic_id: %topic_id%
                Тип чата: %type%
                Топики: %forum%
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
            translation(MinecraftLocales.EN_US, "The code is wrong or expired")
            translation(MinecraftLocales.RU_RU, "Код неверный или устарел")
        },
        @SerialName("admins_only")
        val adminsOnly: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Only admins of the group can bind it")
            translation(MinecraftLocales.RU_RU, "Привязать группу могут только её администраторы")
        },
        @SerialName("success")
        val success: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "✅ The chat is bound to the Minecraft server")
            translation(MinecraftLocales.RU_RU, "✅ Чат привязан к серверу Minecraft")
        },
        @SerialName("bound")
        private val bound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, """Telegram chat "%chat%" is bound""")
            translation(MinecraftLocales.RU_RU, "Чат Telegram «%chat%» привязан")
        },
        @SerialName("bound_topic")
        private val boundTopic: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, """Telegram chat "%chat%", topic "%topic%" is bound""")
            translation(MinecraftLocales.RU_RU, "Чат Telegram «%chat%», топик «%topic%» привязан")
        },
        @SerialName("save_failed")
        val saveFailed: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "❌ Not saved: config.yml of the server has an error")
            translation(MinecraftLocales.RU_RU, "❌ Не сохранено: в config.yml сервера ошибка")
        },
        @SerialName("not_bound")
        private val notBound: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                """Telegram chat "%chat%" is not bound: config.yml has an error (%error%)"""
            )
            translation(MinecraftLocales.RU_RU, "Чат Telegram «%chat%» не привязан: в config.yml ошибка (%error%)")
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
