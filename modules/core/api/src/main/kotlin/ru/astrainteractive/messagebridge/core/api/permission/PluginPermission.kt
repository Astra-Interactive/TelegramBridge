package ru.astrainteractive.messagebridge.core.api.permission

import ru.astrainteractive.astralibs.server.permission.Permission

sealed class PluginPermission(override val value: String) : Permission {
    data object Reload : PluginPermission("tbridge.reload")
    data object UnlinkPlayer : PluginPermission("tbridge.unlink.player")
    data object Setup : PluginPermission("tbridge.setup")
}
