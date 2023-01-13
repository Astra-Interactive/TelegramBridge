package ru.astrainteractive.telegrambridge.utils

import kotlinx.serialization.json.Json.Default.configuration
import org.bukkit.configuration.file.FileConfiguration
import ru.astrainteractive.astralibs.AstraLibs
import ru.astrainteractive.astralibs.utils.getFloat
import ru.astrainteractive.astralibs.configuration.configuration

fun FileConfiguration.cString(path: String, default: String) = configuration(path) {
    this.getString(path, default) ?: default
}

fun FileConfiguration.cBoolean(path: String, default: Boolean) = configuration(path) {
    this.getBoolean(path, default)
}

fun FileConfiguration.dDustOptions(path: String) = configuration(path) {
    this.getString(path)
}

fun FileConfiguration.cDouble(path: String, default: Double) = configuration(path) {
    this.getDouble(path, default)
}
fun FileConfiguration.cFloat(path: String, default: Float) = configuration(path) {
    this.getFloat(path, default)
}