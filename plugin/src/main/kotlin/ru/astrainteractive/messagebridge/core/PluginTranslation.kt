package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.astralibs.string.StringDesc
import ru.astrainteractive.astralibs.string.StringDescExt.replace

@Serializable
data class PluginTranslation(
    @SerialName("general.prefix")
    val prefix: StringDesc.Raw = StringDesc.Raw("#18dbd1[EmpireItems]"),
    @SerialName("general.reload")
    val reload: StringDesc.Raw = StringDesc.Raw("#dbbb18Перезагрузка плагина"),
    @SerialName("general.reload_complete")
    val reloadComplete: StringDesc.Raw = StringDesc.Raw("#42f596Перезагрузка успешно завершена"),
    @SerialName("general.no_permission")
    val noPermission: StringDesc.Raw = StringDesc.Raw("#db2c18У вас нет прав!"),
    @SerialName("messaging.player_join")
    private val playerJoinMessage: StringDesc.Raw = StringDesc.Raw("Игрок %player% присоединился"),
    @SerialName("messaging.player_leave")
    private val playerLeaveMessage: StringDesc.Raw = StringDesc.Raw("Игрок %player% покинул нас"),
    @SerialName("messaging.player_died")
    private val playerDiedMessage: StringDesc.Raw = StringDesc.Raw("Игрок %player% сдох от %cause%"),
    @SerialName("messaging.message.to_telegram")
    private val telegramMessageFormat: StringDesc.Raw = StringDesc.Raw("%player%:\n%message%"),
    @SerialName("messaging.message.to_minecraft")
    private val minecraftMessageFormat: StringDesc.Raw = StringDesc.Raw("#27A1E0%player%: #FFFFFF%message%"),
) {
    fun minecraftMessageFormat(playerName: String, message: String) = minecraftMessageFormat
        .replace("%player%", playerName)
        .replace("%message%", message)

    fun telegramMessageFormat(playerName: String, message: String) = telegramMessageFormat
        .replace("%player%", playerName)
        .replace("%message%", message)

    fun playerDiedMessage(name: String, cause: String) = playerDiedMessage
        .replace("%player%", name)
        .replace("%cause%", cause)

    fun playerLeaveMessage(name: String) = playerLeaveMessage
        .replace("%player%", name)

    fun playerJoinMessage(name: String) = playerJoinMessage
        .replace("%player%", name)
}
