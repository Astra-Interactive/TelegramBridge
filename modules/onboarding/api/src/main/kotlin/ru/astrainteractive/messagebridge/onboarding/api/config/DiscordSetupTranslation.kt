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
            Инструкция на английском: https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/discord.md
            """.trimIndent()
        )
    },
    @SerialName("bind")
    val bind: Bind = Bind()
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
}
