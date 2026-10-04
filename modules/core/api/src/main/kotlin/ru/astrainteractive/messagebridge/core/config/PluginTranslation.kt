package ru.astrainteractive.messagebridge.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replace
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

/**
 * Texts of the plugin, grouped by the feature that sends them. Every text has a default, so the plugin works
 * without `translations.yml` and a missing key keeps its default.
 */
@Serializable
data class PluginTranslation(
    @SerialName("command_error")
    val commandError: CommandError = CommandError(),
    @SerialName("reload")
    val reload: Reload = Reload(),
    @SerialName("chat")
    val chat: Chat = Chat(),
    @SerialName("player")
    val player: Player = Player(),
    @SerialName("server")
    val server: Server = Server(),
    @SerialName("online_players")
    val onlinePlayers: OnlinePlayers = OnlinePlayers(),
    @SerialName("link")
    val link: Link = Link(),
    @SerialName("unlink")
    val unlink: Unlink = Unlink()
) {
    @Serializable
    data class CommandError(
        @SerialName("no_permission")
        val noPermission: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#db2c18You don't have permission!")
            translation(MinecraftLocales.RU_RU, "&#db2c18У вас нет прав!")
        },
        @SerialName("wrong_usage")
        val wrongUsage: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#db2c18Wrong usage!")
            translation(MinecraftLocales.RU_RU, "&#db2c18Неверное использование!")
        },
        @SerialName("only_player_command")
        val onlyPlayerCommand: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#db2c18This command is for players only!")
            translation(MinecraftLocales.RU_RU, "&#db2c18Эта команда только для игроков!")
        },
        @SerialName("player_not_found")
        val playerNotFound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#db2c18Player not found!")
            translation(MinecraftLocales.RU_RU, "&#db2c18Игрок не найден!")
        },
        @SerialName("invalid_argument")
        val invalidArgument: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#db2c18Invalid argument value!")
            translation(MinecraftLocales.RU_RU, "&#db2c18Неверное значение аргумента!")
        },
        @SerialName("unknown_error")
        val unknownError: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#db2c18The command failed with an unknown error")
            translation(MinecraftLocales.RU_RU, "&#db2c18Команда завершилась с неизвестной ошибкой")
        }
    )

    @Serializable
    data class Reload(
        @SerialName("started")
        val started: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#dbbb18Reloading the plugin")
            translation(MinecraftLocales.RU_RU, "&#dbbb18Перезагрузка плагина")
        },
        @SerialName("completed")
        val completed: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596Reload complete")
            translation(MinecraftLocales.RU_RU, "&#42f596Перезагрузка успешно завершена")
        }
    )

    /** Messages relayed between Minecraft and the Telegram or Discord chat. */
    @Serializable
    data class Chat(
        @SerialName("to_minecraft")
        private val toMinecraft: LocalizedText = LocalizedText.shared("[%from%] &#27A1E0%player%: &#FFFFFF%message%"),
        @SerialName("to_minecraft_reply")
        private val toMinecraftReply: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "[%from%] &#27A1E0%player% " +
                    "<hover:show_text:'&#8A8A8AReply to &#27A1E0%reply_player%&#8A8A8A:<newline>" +
                    "&#FFFFFF%reply_message%'>&#8A8A8A↪ %reply_player%</hover>&#27A1E0: &#FFFFFF%message%"
            )
            translation(
                MinecraftLocales.RU_RU,
                "[%from%] &#27A1E0%player% " +
                    "<hover:show_text:'&#8A8A8AОтвет на сообщение &#27A1E0%reply_player%&#8A8A8A:<newline>" +
                    "&#FFFFFF%reply_message%'>&#8A8A8A↪ %reply_player%</hover>&#27A1E0: &#FFFFFF%message%"
            )
        },
        @SerialName("reply_media")
        private val replyMedia: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "[media]")
            translation(MinecraftLocales.RU_RU, "[медиа]")
        },
        @SerialName("to_telegram")
        private val toTelegram: LocalizedText = LocalizedText.shared("[%from%] %player%:\n%message%"),
        @SerialName("illegal_display_name")
        val illegalDisplayName: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Your name contains characters that are not allowed. Set a @username in your Telegram profile settings."
            )
            translation(
                MinecraftLocales.RU_RU,
                "Ваше имя содержит недопустимые символы. Установите @username в настройках профиля Telegram."
            )
        }
    ) {
        /**
         * [playerName] and [message] come from Telegram or Discord users, so they are inserted as plain text:
         * otherwise anyone in the chat could broadcast a `<click:run_command:…>` to every player.
         */
        fun toMinecraft(
            playerName: String,
            message: String,
            from: String
        ): LocalizableComponent = toMinecraft.replaceAll(
            PlaceholderReplacement.plain("%player%", playerName),
            PlaceholderReplacement.plain("%message%", message),
            PlaceholderReplacement.plain("%from%", from)
        )

        fun toTelegram(
            playerName: String,
            message: String,
            from: String
        ): LocalizableComponent = toTelegram.replaceAll(
            PlaceholderReplacement.plain("%player%", playerName),
            PlaceholderReplacement.plain("%message%", message),
            PlaceholderReplacement.plain("%from%", from)
        )

        private fun String.toReplyPreview(): String {
            if (length <= MAX_REPLY_PREVIEW_LENGTH) return this
            val end = if (this[MAX_REPLY_PREVIEW_LENGTH - 1].isHighSurrogate()) {
                MAX_REPLY_PREVIEW_LENGTH - 1
            } else {
                MAX_REPLY_PREVIEW_LENGTH
            }
            return substring(0, end).trimEnd() + "…"
        }

        fun toMinecraftReply(
            playerName: String,
            message: String,
            from: String,
            replyPlayerName: String,
            replyMessage: String
        ): LocalizableComponent {
            val replyMessageReplacement = if (replyMessage.isBlank()) {
                PlaceholderReplacement(placeholder = "%reply_message%", value = replyMedia)
            } else {
                PlaceholderReplacement.plain("%reply_message%", replyMessage.toReplyPreview())
            }
            return toMinecraftReply.replaceAll(
                PlaceholderReplacement.plain("%player%", playerName),
                PlaceholderReplacement.plain("%message%", message),
                PlaceholderReplacement.plain("%from%", from),
                PlaceholderReplacement.plain("%reply_player%", replyPlayerName),
                replyMessageReplacement
            )
        }

        companion object {
            private const val MAX_REPLY_PREVIEW_LENGTH = 200
        }
    }

    /** Announcements of players joining, leaving and dying, for the Telegram chat. */
    @Serializable
    data class Player(
        @SerialName("joined")
        private val joined: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Player %player% joined")
            translation(MinecraftLocales.RU_RU, "Игрок %player% присоединился")
        },
        @SerialName("joined_first_time")
        private val joinedFirstTime: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "🥳 Player %player% joined for the first time!")
            translation(MinecraftLocales.RU_RU, "🥳 Игрок %player% присоединился впервые!")
        },
        @SerialName("left")
        private val left: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Player %player% left")
            translation(MinecraftLocales.RU_RU, "Игрок %player% покинул нас")
        },
        @SerialName("died")
        private val died: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Player %player% died: %cause%")
            translation(MinecraftLocales.RU_RU, "Игрок %player% сдох от %cause%")
        },
        @SerialName("unknown_death_cause")
        private val unknownDeathCause: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "unknown cause")
            translation(MinecraftLocales.RU_RU, "Просто так")
        }
    ) {
        fun joined(name: String): LocalizableComponent = joined.replace("%player%", name)

        fun joinedFirstTime(name: String): LocalizableComponent = joinedFirstTime.replace("%player%", name)

        fun left(name: String): LocalizableComponent = left.replace("%player%", name)

        /** A death without a known [cause] reads [unknownDeathCause] in the same language. */
        fun died(name: String, cause: String?): LocalizableComponent {
            val causeReplacement = if (cause == null) {
                PlaceholderReplacement(placeholder = "%cause%", value = unknownDeathCause)
            } else {
                PlaceholderReplacement.plain("%cause%", cause)
            }
            return died.replaceAll(PlaceholderReplacement.plain("%player%", name), causeReplacement)
        }
    }

    /** Announcements of the server starting and stopping, for the Telegram chat. */
    @Serializable
    data class Server(
        @SerialName("started")
        val started: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "✅ The server has started")
            translation(MinecraftLocales.RU_RU, "✅ Сервер успешно запущен")
        },
        @SerialName("stopped")
        val stopped: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "🛑 The server has stopped")
            translation(MinecraftLocales.RU_RU, "🛑 Сервер остановлен")
        }
    )

    /** Reply to the online-players command in Telegram and Discord. */
    @Serializable
    data class OnlinePlayers(
        @SerialName("message")
        private val message: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%count% players online now\n%players%")
            translation(MinecraftLocales.RU_RU, "Сейчас онлайн %count% игроков\n%players%")
        }
    ) {
        fun message(count: Int, players: String): LocalizableComponent = message.replaceAll(
            PlaceholderReplacement.plain("%count%", "$count"),
            PlaceholderReplacement.plain("%players%", players)
        )
    }

    @Serializable
    data class Link(
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
        @SerialName("account_linked_to_another_player")
        val accountTaken: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "This account is already linked to another player")
            translation(MinecraftLocales.RU_RU, "Этот аккаунт уже привязан к другому игроку")
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
        @SerialName("not_server_member")
        val notServerMember: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Linking is not possible without being a member of the Discord server")
            translation(MinecraftLocales.RU_RU, "Привязка невозможна, если вы не состоите на Discord-сервере")
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
    data class Unlink(
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
