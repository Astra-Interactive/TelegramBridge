package ru.astrainteractive.messagebridge.core.permission

import ru.astrainteractive.astralibs.server.permission.Permission

sealed class PluginPermission(override val value: String) : Permission {
    data object Reload : PluginPermission("tbridge.reload")
}
