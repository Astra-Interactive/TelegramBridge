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
            translation(MinecraftLocales.EN_US, "The code is wrong or expired")
            translation(MinecraftLocales.RU_RU, "Код неверный или устарел")
        },
        @SerialName("wrong_channel_type")
        val wrongChannelType: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Send the code to a text channel")
            translation(MinecraftLocales.RU_RU, "Отправьте код в текстовый канал")
        },
        @SerialName("no_permission")
        val noPermission: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Only members who can manage this channel can bind it")
            translation(MinecraftLocales.RU_RU, "Привязать канал может только участник с правом управлять им")
        },
        @SerialName("config_broken")
        val configBroken: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Not saved: config.yml of the Minecraft server has an error")
            translation(MinecraftLocales.RU_RU, "Не сохранено: в config.yml сервера Minecraft ошибка")
        },
        @SerialName("bound_channel")
        val boundChannel: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "The channel is bound to the Minecraft server")
            translation(MinecraftLocales.RU_RU, "Канал привязан к серверу Minecraft")
        },
        @SerialName("bound")
        private val bound: LocalizedText = LocalizedText.build {
            translation(MinecraftLocales.EN_US, "Discord channel #%channel% on %guild% is bound")
            translation(MinecraftLocales.RU_RU, "Канал Discord #%channel% на сервере %guild% привязан")
        }
    ) {
        fun bound(channel: String, guild: String): LocalizableComponent = bound.replaceAll(
            PlaceholderReplacement.plain("%channel%", channel),
            PlaceholderReplacement.plain("%guild%", guild)
        )
    }
}
