package ru.astrainteractive.messagebridge.core.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replace
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.core.api.util.ellipsize

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
    @SerialName("dao")
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
        private val toMinecraft: LocalizedText = LocalizedText.shared("[%from%] &#27A1E0%dao%: &#FFFFFF%message%"),
        @SerialName("to_minecraft_reply")
        private val toMinecraftReply: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "[%from%] &#27A1E0%dao% " +
                    "<hover:show_text:'&#8A8A8AReply to &#27A1E0%reply_player%&#8A8A8A:<newline>" +
                    "&#FFFFFF%reply_message%'>&#8A8A8A↪ %reply_player%</hover>&#27A1E0: &#FFFFFF%message%"
            )
            translation(
                MinecraftLocales.RU_RU,
                "[%from%] &#27A1E0%dao% " +
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
        private val toTelegram: LocalizedText = LocalizedText.shared("[%from%] %dao%:\n%message%"),
        @SerialName("to_telegram_reply")
        private val toTelegramReply: LocalizedText = LocalizedText.shared("[%from%] %dao%:\n%quote%\n%message%"),
        @SerialName("reply_quote")
        private val replyQuote: LocalizedText = LocalizedText.shared("%reply_player%: %reply_message%"),
        @SerialName("reply_server")
        val replyServer: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "[server]")
            translation(MinecraftLocales.RU_RU, "[сервер]")
        },
        @SerialName("to_discord_username")
        private val toDiscordUsername: LocalizedText = LocalizedText.shared("[%from%] %dao%"),
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
        },
        @SerialName("anonymous_author")
        val anonymousAuthor: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Anonymous")
            translation(MinecraftLocales.RU_RU, "Анонимус")
        }
    ) {
        /**
         * [playerName] and [message] come from Telegram or Discord users, so they are inserted as plain text:
         * otherwise anyone in the chat could broadcast a `<click:run_command:…>` to every dao.
         */
        fun toMinecraft(
            playerName: String,
            message: String,
            from: String
        ): LocalizableComponent = toMinecraft.replaceAll(
            PlaceholderReplacement.plain("%dao%", playerName),
            PlaceholderReplacement.plain("%message%", message),
            PlaceholderReplacement.plain("%from%", from)
        )

        fun toTelegram(
            playerName: String,
            message: String,
            from: String
        ): LocalizableComponent = toTelegram.replaceAll(
            PlaceholderReplacement.plain("%dao%", playerName),
            PlaceholderReplacement.plain("%message%", message),
            PlaceholderReplacement.plain("%from%", from)
        )

        fun toTelegramReply(
            playerName: String,
            message: String,
            from: String,
            quote: String
        ): LocalizableComponent = toTelegramReply.replaceAll(
            PlaceholderReplacement.plain("%dao%", playerName),
            PlaceholderReplacement.plain("%message%", message),
            PlaceholderReplacement.plain("%from%", from),
            PlaceholderReplacement.plain("%quote%", quote)
        )

        fun replyQuote(
            replyPlayerName: String,
            replyMessage: String
        ): LocalizableComponent {
            val replyMessageReplacement = if (replyMessage.isBlank()) {
                PlaceholderReplacement(placeholder = "%reply_message%", value = replyMedia)
            } else {
                PlaceholderReplacement.plain("%reply_message%", replyMessage)
            }
            return replyQuote.replaceAll(
                PlaceholderReplacement.plain("%reply_player%", replyPlayerName),
                replyMessageReplacement
            )
        }

        fun toDiscordUsername(
            playerName: String,
            from: String
        ): LocalizableComponent = toDiscordUsername.replaceAll(
            PlaceholderReplacement.plain("%dao%", playerName),
            PlaceholderReplacement.plain("%from%", from)
        )

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
                PlaceholderReplacement.plain("%reply_message%", replyMessage.ellipsize(MAX_REPLY_PREVIEW_LENGTH))
            }
            return toMinecraftReply.replaceAll(
                PlaceholderReplacement.plain("%dao%", playerName),
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
            translation(MinecraftLocales.EN_US, "Player %dao% joined")
            translation(MinecraftLocales.RU_RU, "Игрок %dao% присоединился")
        },
        @SerialName("joined_first_time")
        private val joinedFirstTime: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "🥳 Player %dao% joined for the first time!")
            translation(MinecraftLocales.RU_RU, "🥳 Игрок %dao% присоединился впервые!")
        },
        @SerialName("left")
        private val left: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Player %dao% left")
            translation(MinecraftLocales.RU_RU, "Игрок %dao% покинул нас")
        },
        @SerialName("died")
        private val died: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Player %dao% died: %cause%")
            translation(MinecraftLocales.RU_RU, "Игрок %dao% сдох от %cause%")
        },
        @SerialName("unknown_death_cause")
        private val unknownDeathCause: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "unknown cause")
            translation(MinecraftLocales.RU_RU, "Просто так")
        },
        @SerialName("discord_joined")
        private val discordJoined: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%dao% joined")
            translation(MinecraftLocales.RU_RU, "%dao% присоединился")
        },
        @SerialName("discord_joined_first_time")
        private val discordJoinedFirstTime: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%dao% joined for the first time!")
            translation(MinecraftLocales.RU_RU, "%dao% присоединился впервые!")
        },
        @SerialName("discord_left")
        private val discordLeft: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%dao% left")
            translation(MinecraftLocales.RU_RU, "%dao% покинул нас")
        },
        @SerialName("discord_died_of_unknown_cause")
        private val discordDiedOfUnknownCause: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%dao% died =))")
            translation(MinecraftLocales.RU_RU, "%dao% сдох =))")
        }
    ) {
        fun joined(name: String): LocalizableComponent = joined.replace("%dao%", name)

        fun joinedFirstTime(name: String): LocalizableComponent = joinedFirstTime.replace("%dao%", name)

        fun left(name: String): LocalizableComponent = left.replace("%dao%", name)

        fun discordJoined(name: String): LocalizableComponent = discordJoined.replace("%dao%", name)

        fun discordJoinedFirstTime(name: String): LocalizableComponent = discordJoinedFirstTime.replace("%dao%", name)

        fun discordLeft(name: String): LocalizableComponent = discordLeft.replace("%dao%", name)

        fun discordDiedOfUnknownCause(name: String): LocalizableComponent {
            return discordDiedOfUnknownCause.replace("%dao%", name)
        }

        /** A death without a known [cause] reads [unknownDeathCause] in the same language. */
        fun died(name: String, cause: String?): LocalizableComponent {
            val causeReplacement = if (cause == null) {
                PlaceholderReplacement(placeholder = "%cause%", value = unknownDeathCause)
            } else {
                PlaceholderReplacement.plain("%cause%", cause)
            }
            return died.replaceAll(PlaceholderReplacement.plain("%dao%", name), causeReplacement)
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
        },
        @SerialName("discord_started")
        val discordStarted: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "✅ **The server has started**")
            translation(MinecraftLocales.RU_RU, "✅ **Сервер успешно запущен**")
        },
        @SerialName("discord_stopped")
        val discordStopped: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "🛑 **The server has stopped**")
            translation(MinecraftLocales.RU_RU, "🛑 **Сервер остановлен**")
        },
        @SerialName("discord_topic_starting")
        val discordTopicStarting: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The server has just started...")
            translation(MinecraftLocales.RU_RU, "Сервер только запустился...")
        }
    )

    /** Reply to the online-players command in Telegram and Discord. */
    @Serializable
    data class OnlinePlayers(
        @SerialName("message")
        private val message: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "%count% players online now\n%players%")
            translation(MinecraftLocales.RU_RU, "Сейчас онлайн %count% игроков\n%players%")
        },
        @SerialName("discord_topic")
        private val discordTopic: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Players online: %count%")
            translation(MinecraftLocales.RU_RU, "Игроков в сети: %count%")
        }
    ) {
        fun message(count: Int, players: String): LocalizableComponent = message.replaceAll(
            PlaceholderReplacement.plain("%count%", "$count"),
            PlaceholderReplacement.plain("%players%", players)
        )

        fun discordTopic(count: Int): LocalizableComponent = discordTopic.replace("%count%", "$count")
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
            translation(MinecraftLocales.EN_US, "This account is already linked to another dao")
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
        },
        @SerialName("user_info")
        private val userInfo: LocalizedText = LocalizedText.shared(
            "DiscordID: %discord_id%; telegramUsername: %telegram_username%; minecraftUUID: %minecraft_uuid%"
        )
    ) {
        fun codeCreated(code: Int): LocalizableComponent = codeCreated.replace("%code%", "$code")

        fun userInfo(
            discordId: String,
            telegramUsername: String,
            minecraftUuid: String
        ): LocalizableComponent = userInfo.replaceAll(
            PlaceholderReplacement.plain("%discord_id%", discordId),
            PlaceholderReplacement.plain("%telegram_username%", telegramUsername),
            PlaceholderReplacement.plain("%minecraft_uuid%", minecraftUuid)
        )
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
            translation(MinecraftLocales.EN_US, "The dao's account is not linked")
            translation(MinecraftLocales.RU_RU, "Аккаунт игрока не привязан")
        },
        @SerialName("player_success")
        val playerSuccess: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "&#42f596The dao's account is unlinked")
            translation(MinecraftLocales.RU_RU, "&#42f596Привязка игрока успешно удалена")
        }
    )
}
