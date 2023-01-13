package ru.astrainteractive.telegrambridge.utils

import ru.astrainteractive.astralibs.utils.IPermission

sealed class Permission(override val value: String) : IPermission {
    object Reload : Permission("tbridge.reload")
}