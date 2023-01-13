package ru.astrainteractive.telegrambridge.utils

import ru.astrainteractive.astralibs.file_manager.FileManager
import ru.astrainteractive.astralibs.utils.BaseTranslation

/**
 * All translation stored here
 */
class PluginTranslation : BaseTranslation() {
    /**
     * This is a default translation file. Don't forget to create translation.yml in resources of the plugin
     */
    protected override val translationFile: FileManager = FileManager("translations.yml")


    //General
    val prefix = translationValue("general.prefix", "#18dbd1[EmpireItems]")
    val reload = translationValue("general.reload", "#dbbb18Перезагрузка плагина")
    val reloadComplete = translationValue("general.reload_complete", "#42f596Перезагрузка успешно завершена")
    val noPermission = translationValue("general.no_permission", "#db2c18У вас нет прав!")

    private val playerJoinMessage = translationValue("messaging.player_join", "Игрок %player% присоединился")
    fun playerJoinMessage(name: String) = playerJoinMessage.replace("%player%", name)

    private val playerLeaveMessage = translationValue("messaging.player_leave", "Игрок %player% покинул нас")
    fun playerLeaveMessage(name: String) = playerLeaveMessage.replace("%player%", name)

    private val playerDiedMessage = translationValue("messaging.player_died", "Игрок %player% сдох от %cause%")
    fun playerDiedMessage(name: String, cause: String) = playerDiedMessage
        .replace("%player%", name)
        .replace("%cause%", cause)

    private val telegramMessageFormat = translationValue("messaging.message.to_telegram", "%player%:\n%message%")
    fun telegramMessageFormat(playerName: String, message: String) = telegramMessageFormat
        .replace("%player%", playerName)
        .replace("%message%", message)

    private val minecraftMessageFormat = translationValue("messaging.message.to_minecraft", "#27A1E0%player%: #FFFFFF%message%")
    fun minecraftMessageFormat(playerName: String, message: String) = minecraftMessageFormat
        .replace("%player%", playerName)
        .replace("%message%", message)

    private val discordMessageFormat = translationValue("messaging.message.to_discord", "%player%: %message%")
    fun discordMessageFormat(playerName: String, message: String) = discordMessageFormat
        .replace("%player%", playerName)
        .replace("%message%", message)

}


