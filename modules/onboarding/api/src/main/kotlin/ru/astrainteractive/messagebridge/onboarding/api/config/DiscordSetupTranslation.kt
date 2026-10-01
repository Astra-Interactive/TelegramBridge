package ru.astrainteractive.messagebridge.onboarding.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.component.PlaceholderReplacement
import ru.astrainteractive.astralibs.localization.component.replaceAll
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

@Serializable
data class DiscordSetupTranslation(
    @SerialName("guide")
    val guide: LocalizedText = LocalizedText.build {
        translation(
            MinecraftLocales.EN_US,
            """
            Discord is not configured yet:
             1. Create an application at https://discord.com/developers/applications, open Bot, copy the token
                and turn on Message Content Intent. When link in config.yml gives roles for linking, turn on
                Server Members Intent too: it lets the bot take the roles back from players who leave the server
             2. In the server console run: mb discord token <token>
             3. Run mb discord invite and open the link to add the bot to your server
             4. Run mb discord bind and send the code it shows into the channel
             5. Check everything with mb discord check
            Guide: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/discord.md
            """.trimIndent()
        )
        translation(
            MinecraftLocales.RU_RU,
            """
            Discord ещё не настроен:
             1. Создайте приложение на https://discord.com/developers/applications, откройте Bot, скопируйте токен
                и включите Message Content Intent. Если link в config.yml выдаёт роли за привязку, включите
                и Server Members Intent: так бот заберёт роли у игроков, которые ушли с сервера
             2. В консоли сервера выполните: mb discord token <токен>
             3. Выполните mb discord invite и откройте ссылку, чтобы добавить бота на сервер
             4. Выполните mb discord bind и отправьте код в нужный канал
             5. Проверьте настройку: mb discord check
            Инструкция на английском: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/discord.md
            """.trimIndent()
        )
    },
    @SerialName("bind")
    val bind: Bind = Bind(),
    @SerialName("check")
    val check: Check = Check()
) {
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
        @SerialName("config_broken")
        val configBroken: LocalizedText = LocalizedText.build {
            translation(
                MinecraftLocales.EN_US,
                "config.yml of the Minecraft server has an error, so this channel is not saved. Fix the file, " +
                    "run /mb reload and then /mb discord bind again"
            )
            translation(
                MinecraftLocales.RU_RU,
                "В config.yml сервера Minecraft ошибка, поэтому канал не сохранён. Исправьте файл, выполните " +
                    "/mb reload, а затем снова /mb discord bind"
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
        @SerialName("intents_ok")
        private val intentsOk: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Discord lets the bot use %intents%")
            translation(MinecraftLocales.RU_RU, "Discord разрешает боту %intents%")
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

        fun intentsOk(intents: String): LocalizableComponent = intentsOk.replaceAll(
            PlaceholderReplacement.plain("%intents%", intents)
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
