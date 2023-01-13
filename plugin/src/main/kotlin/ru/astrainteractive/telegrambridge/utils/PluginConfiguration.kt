package ru.astrainteractive.telegrambridge.utils

import org.bukkit.configuration.file.FileConfiguration
import ru.astrainteractive.astralibs.configuration.getValue

class PluginConfiguration(fc: FileConfiguration) {
    val token by fc.cString("telegram.token", "")
    val channelID by fc.cString("telegram.chat_id", "0")
    val topicID by fc.cString("telegram.topic_id", "0")

    val displayJoinMessage by fc.cBoolean("chat.join", true)
    val displayLeaveMessage by fc.cBoolean("chat.leave", true)
    val displayDeathMessage by fc.cBoolean("chat.death", true)
}


