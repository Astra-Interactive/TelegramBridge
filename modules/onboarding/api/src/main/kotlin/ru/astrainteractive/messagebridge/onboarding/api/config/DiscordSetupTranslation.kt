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
