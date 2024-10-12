package ru.astrainteractive.messagebridge.core

import ru.astrainteractive.astralibs.permission.Permission

sealed class PluginPermission(override val value: String) : Permission {
    data object Reload : PluginPermission("tbridge.reload")
}
