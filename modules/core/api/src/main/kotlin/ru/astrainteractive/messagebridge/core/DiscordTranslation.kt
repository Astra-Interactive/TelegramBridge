package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

/**
 * Texts about setting up the Discord bot and the errors it runs into. They are shown in the console, in the game
 * and in Discord, so they have no colors.
 */
@Serializable
data class DiscordTranslation(
    @SerialName("guide")
    val guide: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            """
            Discord is not configured yet:
             1. Create an application at https://discord.com/developers/applications, open Bot, copy the token
                and turn on Message Content Intent
             2. In the server console run: mb discord token <token>
             3. Run mb discord invite and open the link to add the bot to your server
             4. Run mb discord bind and send the code it shows into the channel
             5. Check everything with mb discord check
            Guide: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/en/discord.md
            """.trimIndent()
        )
        translation(
            MinecraftLocales.RU_RU,
            """
            Discord ещё не настроен:
             1. Создайте приложение на https://discord.com/developers/applications, откройте Bot, скопируйте токен
                и включите Message Content Intent
             2. В консоли сервера выполните: mb discord token <токен>
             3. Выполните mb discord invite и откройте ссылку, чтобы добавить бота на сервер
             4. Выполните mb discord bind и отправьте код в нужный канал
             5. Проверьте настройку: mb discord check
            Инструкция: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/ru/discord.md
            """.trimIndent()
        )
    },
    @SerialName("errors")
    val errors: Errors = Errors(),
    @SerialName("bind")
    val bind: Bind = Bind(),
    @SerialName("check")
    val check: Check = Check()
) {
    /** Why the bot could not connect or deliver a message, and what to do about it. */
    @Serializable
    data class Errors(
        @SerialName("invalid_token")
        val invalidToken: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Discord rejected the bot token. Open https://discord.com/developers/applications, choose your " +
                    "application, open Bot, press Reset Token and run /mb discord token <token> with the new token"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Discord не принял токен бота. Откройте https://discord.com/developers/applications, выберите " +
                    "приложение, откройте Bot, нажмите Reset Token и выполните /mb discord token <токен> " +
                    "с новым токеном"
            )
        },
        @SerialName("missing_intent")
        val missingIntent: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Discord refused the connection because Message Content Intent is off. Open " +
                    "https://discord.com/developers/applications, choose your application, open Bot, turn on " +
                    "Message Content Intent under Privileged Gateway Intents, save the changes and run /mb reload"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Discord не пустил бота: выключен Message Content Intent. Откройте " +
                    "https://discord.com/developers/applications, выберите приложение, откройте Bot, включите " +
                    "Message Content Intent в разделе Privileged Gateway Intents, сохраните изменения " +
                    "и выполните /mb reload"
            )
        },
        @SerialName("channel_not_set")
        val channelNotSet: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The Discord channel is not set. Run /mb discord bind and send the code into the channel, " +
                    "or set its id with /mb discord channel <channel_id>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Канал Discord не задан. Выполните /mb discord bind и отправьте код в нужный канал " +
                    "или укажите его id: /mb discord channel <id_канала>"
            )
        },
        @SerialName("channel_not_found")
        private val channelNotFound: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot cannot find the channel %channel_id%: the bot is not on that server or the id is wrong. " +
                    "Add the bot with /mb discord invite, then run /mb discord bind and send the code into the channel"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бот не видит канал %channel_id%: бота нет на этом сервере или id указан неверно. " +
                    "Добавьте бота через /mb discord invite, затем выполните /mb discord bind и отправьте код " +
                    "в нужный канал"
            )
        },
        @SerialName("missing_permission")
        private val missingPermission: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot lacks the \"%permission%\" permission in the channel. Give it to the bot's role " +
                    "in the channel settings or add the bot again with /mb discord invite"
            )
            translation(
                MinecraftLocales.RU_RU,
                "У бота нет права «%permission%» в канале. Выдайте его роли бота в настройках канала " +
                    "или добавьте бота заново через /mb discord invite"
            )
        },
        @SerialName("socks_not_supported")
        val socksNotSupported: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Discord works only through an HTTP proxy, but a SOCKS5 proxy is set. Set an HTTP proxy with " +
                    "/mb discord proxy http <host> <port> [username] [password] or turn the proxy off " +
                    "with /mb discord proxy off"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Discord работает только через HTTP-прокси, а указан SOCKS5. Задайте HTTP-прокси: " +
                    "/mb discord proxy http <хост> <порт> [логин] [пароль] или отключите прокси: " +
                    "/mb discord proxy off"
            )
        },
        @SerialName("network")
        private val network: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Could not reach Discord: %error%. If Discord is blocked in your country, set an HTTP proxy " +
                    "with /mb discord proxy http <host> <port> [username] [password]"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удалось подключиться к Discord: %error%. Если Discord заблокирован в вашей стране, " +
                    "задайте HTTP-прокси: /mb discord proxy http <хост> <порт> [логин] [пароль]"
            )
        },
        @SerialName("network_via_proxy")
        private val networkViaProxy: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Could not reach Discord through the proxy %proxy%: %error%. Check that the proxy works and " +
                    "its address, username and password are right, or change it with " +
                    "/mb discord proxy http <host> <port> [username] [password]"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Не удалось подключиться к Discord через прокси %proxy%: %error%. Проверьте, что прокси " +
                    "работает, а адрес, логин и пароль указаны верно, или замените его: " +
                    "/mb discord proxy http <хост> <порт> [логин] [пароль]"
            )
        },
        @SerialName("unknown")
        private val unknown: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Discord error: %error%. Common problems and their fixes: " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/en/troubleshooting.md"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Ошибка Discord: %error%. Частые проблемы и их решения: " +
                    "https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/ru/troubleshooting.md"
            )
        }
    ) {
        fun channelNotFound(channelId: String): LocalizableComponent = channelNotFound.replaceAll(
            PlaceholderReplacement.plain("%channel_id%", channelId)
        )

        fun missingPermission(permission: String): LocalizableComponent = missingPermission.replaceAll(
            PlaceholderReplacement.plain("%permission%", permission)
        )

        fun network(error: String): LocalizableComponent = network.replaceAll(
            PlaceholderReplacement.plain("%error%", error)
        )

        fun networkViaProxy(proxy: String, error: String): LocalizableComponent = networkViaProxy.replaceAll(
            PlaceholderReplacement.plain("%proxy%", proxy),
            PlaceholderReplacement.plain("%error%", error)
        )

        fun unknown(error: String): LocalizableComponent = unknown.replaceAll(
            PlaceholderReplacement.plain("%error%", error)
        )
    }

    /** Replies to `!bind <code>` sent into a Discord channel. */
    @Serializable
    data class Bind(
        @SerialName("code_invalid")
        val codeInvalid: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The code is invalid or expired. Run /mb discord bind in the server console or in the game " +
                    "to get a new one"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Код неверный или устарел. Выполните /mb discord bind в консоли сервера или в игре, " +
                    "чтобы получить новый"
            )
        },
        @SerialName("wrong_channel_type")
        val wrongChannelType: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Send the code into a text channel of the server, not into a thread or a voice channel chat"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Отправьте код в текстовый канал сервера, а не в ветку или чат голосового канала"
            )
        },
        @SerialName("no_permission")
        val noPermission: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "Only members who can manage this channel can connect it to the Minecraft server"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Подключить канал к серверу Minecraft может только участник с правом управлять этим каналом"
            )
        },
        @SerialName("bound_channel")
        val boundChannel: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "This channel is now connected to the Minecraft server")
            translation(MinecraftLocales.RU_RU, "Этот канал подключён к серверу Minecraft")
        },
        @SerialName("bound")
        private val bound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Discord channel #%channel% on %guild% is connected")
            translation(MinecraftLocales.RU_RU, "Канал Discord #%channel% на сервере %guild% подключён")
        }
    ) {
        fun bound(channel: String, guild: String): LocalizableComponent = bound.replaceAll(
            PlaceholderReplacement.plain("%channel%", channel),
            PlaceholderReplacement.plain("%guild%", guild)
        )
    }

    /** Lines of `/mb discord check`. */
    @Serializable
    data class Check(
        @SerialName("token_missing")
        val tokenMissing: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot token is not set. Create a bot at https://discord.com/developers/applications " +
                    "and run /mb discord token <token>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Токен бота не задан. Создайте бота на https://discord.com/developers/applications " +
                    "и выполните /mb discord token <токен>"
            )
        },
        @SerialName("connecting")
        val connecting: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot is still connecting to Discord. Run /mb discord check again in a minute; if it does " +
                    "not connect, Discord may be blocked: set a proxy with /mb discord proxy http <host> <port>"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бот ещё подключается к Discord. Повторите /mb discord check через минуту; если он так " +
                    "и не подключится, возможно, Discord заблокирован: задайте прокси через " +
                    "/mb discord proxy http <хост> <порт>"
            )
        },
        @SerialName("bot_works")
        private val botWorks: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Bot %bot% is connected to Discord")
            translation(MinecraftLocales.RU_RU, "Бот %bot% подключён к Discord")
        },
        @SerialName("no_guilds")
        val noGuilds: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot is not on any Discord server. Run /mb discord invite and open the link to add it"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бот не добавлен ни на один сервер Discord. Выполните /mb discord invite и откройте ссылку, " +
                    "чтобы добавить его"
            )
        },
        @SerialName("permissions_ok")
        private val permissionsOk: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The bot has every permission it needs in #%channel% on %guild%")
            translation(MinecraftLocales.RU_RU, "У бота есть все нужные права в канале #%channel% на сервере %guild%")
        },
        @SerialName("missing_permissions")
        private val missingPermissions: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot lacks permissions in #%channel%: %permissions%. Give them to the bot's role " +
                    "in the channel settings or add the bot again with /mb discord invite"
            )
            translation(
                MinecraftLocales.RU_RU,
                "У бота не хватает прав в канале #%channel%: %permissions%. Выдайте их роли бота в настройках " +
                    "канала или добавьте бота заново через /mb discord invite"
            )
        },
        @SerialName("link_role_not_found")
        private val linkRoleNotFound: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The link role %role% is not found on %guild%, so linked players get no role. Put the id " +
                    "of the role into link.linkDiscordRole in config.yml"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Роль за привязку %role% не найдена на сервере %guild%, поэтому привязавшиеся игроки не получат " +
                    "роль. Укажите id роли в link.linkDiscordRole в config.yml"
            )
        },
        @SerialName("link_role_no_permission")
        val linkRoleNoPermission: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot lacks the \"Manage Roles\" permission, so it cannot give the link role. Give it " +
                    "to the bot's role in Server Settings → Roles"
            )
            translation(
                MinecraftLocales.RU_RU,
                "У бота нет права «Управлять ролями», поэтому он не может выдавать роль за привязку. " +
                    "Выдайте его роли бота: Настройки сервера → Роли"
            )
        },
        @SerialName("link_role_above_bot")
        private val linkRoleAboveBot: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "The bot cannot give the role %role% because it is not below the bot's own role. " +
                    "In Server Settings → Roles, drag the bot's role above %role%"
            )
            translation(
                MinecraftLocales.RU_RU,
                "Бот не может выдавать роль %role%, потому что она не ниже роли самого бота. " +
                    "В Настройках сервера → Роли перетащите роль бота выше %role%"
            )
        },
        @SerialName("link_role_ok")
        private val linkRoleOk: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The bot can give the role %role% to linked players")
            translation(MinecraftLocales.RU_RU, "Бот может выдавать роль %role% привязавшимся игрокам")
        },
        @SerialName("test_message")
        val testMessage: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "MessageBridge check: the bot can send messages to this channel")
            translation(MinecraftLocales.RU_RU, "Проверка MessageBridge: бот может отправлять сообщения в этот канал")
        },
        @SerialName("test_message_sent")
        private val testMessageSent: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "A test message was sent to #%channel%")
            translation(MinecraftLocales.RU_RU, "Тестовое сообщение отправлено в канал #%channel%")
        },
        @SerialName("test_message_failed")
        private val testMessageFailed: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Could not send a test message: %reason%")
            translation(MinecraftLocales.RU_RU, "Не удалось отправить тестовое сообщение: %reason%")
        }
    ) {
        fun botWorks(bot: String): LocalizableComponent = botWorks.replaceAll(
            PlaceholderReplacement.plain("%bot%", bot)
        )

        fun permissionsOk(channel: String, guild: String): LocalizableComponent = permissionsOk.replaceAll(
            PlaceholderReplacement.plain("%channel%", channel),
            PlaceholderReplacement.plain("%guild%", guild)
        )

        fun missingPermissions(channel: String, permissions: String): LocalizableComponent =
            missingPermissions.replaceAll(
                PlaceholderReplacement.plain("%channel%", channel),
                PlaceholderReplacement.plain("%permissions%", permissions)
            )

        fun linkRoleNotFound(role: String, guild: String): LocalizableComponent = linkRoleNotFound.replaceAll(
            PlaceholderReplacement.plain("%role%", role),
            PlaceholderReplacement.plain("%guild%", guild)
        )

        fun linkRoleAboveBot(role: String): LocalizableComponent = linkRoleAboveBot.replaceAll(
            PlaceholderReplacement.plain("%role%", role)
        )

        fun linkRoleOk(role: String): LocalizableComponent = linkRoleOk.replaceAll(
            PlaceholderReplacement.plain("%role%", role)
        )

        fun testMessageSent(channel: String): LocalizableComponent = testMessageSent.replaceAll(
            PlaceholderReplacement.plain("%channel%", channel)
        )

        fun testMessageFailed(reason: LocalizableComponent): LocalizableComponent = testMessageFailed.replaceAll(
            PlaceholderReplacement(placeholder = "%reason%", value = reason)
        )
    }
}
