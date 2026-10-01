package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText

/** Replies of the /mb setup commands. */
@Serializable
data class SetupTranslation(
    @SerialName("help")
    val help: LocalizedText = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&#42f596MessageBridge: /mb status, /mb telegram, /mb discord, /mb reload")
        translation(MinecraftLocales.RU_RU, "&#42f596MessageBridge: /mb status, /mb telegram, /mb discord, /mb reload")
    }
)
