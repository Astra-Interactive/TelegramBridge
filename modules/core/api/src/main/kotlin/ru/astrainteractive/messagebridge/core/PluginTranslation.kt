package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replace
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

@Serializable
data class PluginTranslation(
    @SerialName("general.prefix")
    val prefix: LocalizedText = LocalizedText.shared("&#18dbd1[EmpireItems]"),
    @SerialName("general.reload")
    val reload: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "&#dbbb18Перезагрузка плагина")
        translation(MinecraftLocales.EN_US, "&#dbbb18Reloading the plugin")
    },
    @SerialName("general.reload_complete")
    val reloadComplete: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "&#42f596Перезагрузка успешно завершена")
        translation(MinecraftLocales.EN_US, "&#42f596Reload complete")
    },
    @SerialName("general.no_permission")
    val noPermission: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "&#db2c18У вас нет прав!")
        translation(MinecraftLocales.EN_US, "&#db2c18You don't have permission!")
    },
    @SerialName("messaging.player_join")
    private val playerJoinMessage: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "Игрок %player% присоединился")
        translation(MinecraftLocales.EN_US, "Player %player% joined")
    },
    @SerialName("messaging.player_join_first_time")
    private val playerJoinMessageFirstTime: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "🥳 Игрок %player% присоединился впервые!")
        translation(MinecraftLocales.EN_US, "🥳 Player %player% joined for the first time!")
    },
    @SerialName("messaging.player_leave")
    private val playerLeaveMessage: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "Игрок %player% покинул нас")
        translation(MinecraftLocales.EN_US, "Player %player% left")
    },
    @SerialName("messaging.player_died")
    private val playerDiedMessage: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "Игрок %player% сдох от %cause%")
        translation(MinecraftLocales.EN_US, "Player %player% died: %cause%")
    },
    @SerialName("messaging.player_died_unknown_cause")
    private val unknownDeathCause: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "Просто так")
        translation(MinecraftLocales.EN_US, "unknown cause")
    },
    @SerialName("messaging.message.to_telegram")
    private val telegramMessageFormat: LocalizedText = LocalizedText.shared("[%from%] %player%:\n%message%"),
    @SerialName("messaging.message.to_minecraft")
    private val minecraftMessageFormat: LocalizedText = LocalizedText.shared(
        "[%from%] &#27A1E0%player%: &#FFFFFF%message%"
    ),
    @SerialName("messaging.online_players")
    private val onlinePlayersMessage: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "Сейчас онлайн %count% игроков\n%players%")
        translation(MinecraftLocales.EN_US, "%count% players online now\n%players%")
    },
    @SerialName("messaging.message.server_open")
    val serverOpenMessage: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "✅ Сервер успешно запущен")
        translation(MinecraftLocales.EN_US, "✅ The server has started")
    },
    @SerialName("messaging.message.server_closed")
    val serverClosedMessage: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.RU_RU, "🛑 Сервер остановлен")
        translation(MinecraftLocales.EN_US, "🛑 The server has stopped")
    },
    @SerialName("messaging.illegal_display_name")
    val illegalDisplayName: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.RU_RU,
            "Ваше имя содержит недопустимые символы. Установите @username в настройках профиля Telegram."
        )
        translation(
            MinecraftLocales.EN_US,
            "Your name contains characters that are not allowed. Set a @username in your Telegram profile settings."
        )
    },
    @SerialName("link")
    val link: Link = Link(),
    @SerialName("unlink")
    val unlink: Unlink = Unlink()
) {
    @Serializable
    data class Unlink(
        @SerialName("success")
        val success: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "&#42f596Привязка успешно удалена")
            translation(MinecraftLocales.EN_US, "&#42f596Your account is unlinked")
        },
        @SerialName("not_linked")
        val notLinked: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "Ваш аккаунт не привязан")
            translation(MinecraftLocales.EN_US, "Your account is not linked")
        },
        @SerialName("player_not_linked")
        val playerNotLinked: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "Аккаунт игрока не привязан")
            translation(MinecraftLocales.EN_US, "The player's account is not linked")
        },
        @SerialName("player_unlink_success")
        val playerUnlinkSuccess: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "&#42f596Привязка игрока успешно удалена")
            translation(MinecraftLocales.EN_US, "&#42f596The player's account is unlinked")
        },
    )

    @Serializable
    data class Link(
        @SerialName("code_created")
        private val codeCreated: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "&#42f596Ваш код: %code%. Используйте /link <code> в TG или Discord.")
            translation(MinecraftLocales.EN_US, "&#42f596Your code: %code%. Use /link <code> in Telegram or Discord.")
        },
        @SerialName("already_linked")
        val alreadyLinked: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "Вы уже привязали аккаунт этим способом")
            translation(MinecraftLocales.EN_US, "You have already linked an account this way")
        },
        @SerialName("no_code_found")
        val noCodeFound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "Код не найден. Используйте /link в игре для создания кода")
            translation(MinecraftLocales.EN_US, "Code not found. Use /link in the game to create one")
        },
        @SerialName("no_username")
        val noUsername: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.RU_RU,
                "У вас не задан username в Telegram. Установите @username в настройках профиля и попробуйте снова."
            )
            translation(
                MinecraftLocales.EN_US,
                "You have no Telegram username. Set a @username in your profile settings and try again."
            )
        },
        @SerialName("unknown_error")
        val unknownError: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "Произошла неизвестная ошибка")
            translation(MinecraftLocales.EN_US, "An unknown error occurred")
        },
        @SerialName("link_success")
        val linkSuccess: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "Привязка прошла успешно")
            translation(MinecraftLocales.EN_US, "Your account is linked")
        },
    ) {
        fun codeCreated(code: Int): LocalizableComponent = codeCreated.replace("%code%", "$code")
    }

    /**
     * [playerName] and [message] come from Telegram or Discord users, so they are inserted as plain text:
     * otherwise anyone in the chat could broadcast a `<click:run_command:…>` to every player.
     */
    fun minecraftMessageFormat(
        playerName: String,
        message: String,
        from: String
    ): LocalizableComponent = minecraftMessageFormat.replaceAll(
        PlaceholderReplacement.plain("%player%", playerName),
        PlaceholderReplacement.plain("%message%", message),
        PlaceholderReplacement.plain("%from%", from)
    )

    fun telegramMessageFormat(
        playerName: String,
        message: String,
        from: String
    ): LocalizableComponent = telegramMessageFormat.replaceAll(
        PlaceholderReplacement.plain("%player%", playerName),
        PlaceholderReplacement.plain("%message%", message),
        PlaceholderReplacement.plain("%from%", from)
    )

    fun onlinePlayersMessage(count: Int, players: String): LocalizableComponent = onlinePlayersMessage.replaceAll(
        PlaceholderReplacement.plain("%count%", "$count"),
        PlaceholderReplacement.plain("%players%", players)
    )

    /** A death without a known [cause] reads [unknownDeathCause] in the same language. */
    fun playerDiedMessage(name: String, cause: String?): LocalizableComponent {
        val causeReplacement = if (cause == null) {
            PlaceholderReplacement(placeholder = "%cause%", value = unknownDeathCause)
        } else {
            PlaceholderReplacement.plain("%cause%", cause)
        }
        return playerDiedMessage.replaceAll(PlaceholderReplacement.plain("%player%", name), causeReplacement)
    }

    fun playerLeaveMessage(name: String): LocalizableComponent = playerLeaveMessage.replace("%player%", name)

    fun playerJoinMessage(name: String): LocalizableComponent = playerJoinMessage.replace("%player%", name)

    fun playerJoinMessageFirstTime(name: String): LocalizableComponent = playerJoinMessageFirstTime
        .replace("%player%", name)
}
