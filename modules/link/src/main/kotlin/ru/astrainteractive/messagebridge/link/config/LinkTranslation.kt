package ru.astrainteractive.messagebridge.link.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.replace
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

@Serializable
internal data class LinkTranslation(
    @SerialName("link")
    val link: Link = Link(),
    @SerialName("unlink")
    val unlink: Unlink = Unlink()
) {
    @Serializable
    internal data class Link(
        @SerialName("code_created")
        private val codeCreated: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Your code: %code%. Use /link <code> in Telegram or Discord.")
            translation(MinecraftLocales.RU_RU, "&#42f596Ваш код: %code%. Используйте /link <code> в TG или Discord.")
        },
        @SerialName("already_linked")
        val alreadyLinked: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "You have already linked an account this way")
            translation(MinecraftLocales.RU_RU, "Вы уже привязали аккаунт этим способом")
        },
        @SerialName("no_code_found")
        val noCodeFound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Code not found. Use /link in the game to create one")
            translation(MinecraftLocales.RU_RU, "Код не найден. Используйте /link в игре для создания кода")
        },
        @SerialName("no_username")
        val noUsername: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "You have no Telegram username. Set a @username in your profile settings and try again."
            )
            translation(
                MinecraftLocales.RU_RU,
                "У вас не задан username в Telegram. Установите @username в настройках профиля и попробуйте снова."
            )
        },
        @SerialName("unknown_error")
        val unknownError: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "An unknown error occurred")
            translation(MinecraftLocales.RU_RU, "Произошла неизвестная ошибка")
        },
        @SerialName("success")
        val success: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Your account is linked")
            translation(MinecraftLocales.RU_RU, "Привязка прошла успешно")
        }
    ) {
        fun codeCreated(code: Int): LocalizableComponent = codeCreated.replace("%code%", "$code")
    }

    @Serializable
    internal data class Unlink(
        @SerialName("success")
        val success: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Your account is unlinked")
            translation(MinecraftLocales.RU_RU, "&#42f596Привязка успешно удалена")
        },
        @SerialName("not_linked")
        val notLinked: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Your account is not linked")
            translation(MinecraftLocales.RU_RU, "Ваш аккаунт не привязан")
        },
        @SerialName("player_not_linked")
        val playerNotLinked: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The player's account is not linked")
            translation(MinecraftLocales.RU_RU, "Аккаунт игрока не привязан")
        },
        @SerialName("player_success")
        val playerSuccess: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596The player's account is unlinked")
            translation(MinecraftLocales.RU_RU, "&#42f596Привязка игрока успешно удалена")
        }
    )
}
