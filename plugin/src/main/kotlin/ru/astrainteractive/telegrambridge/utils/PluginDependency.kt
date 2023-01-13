package ru.astrainteractive.telegrambridge.utils

sealed interface PluginDependency<T> {
    class PluginConnected<T>(val value: T) : PluginDependency<T>
    object PluginNotInstalled : PluginDependency<Nothing>
    fun getOrNull() = (this as? PluginConnected<T>)?.value
}