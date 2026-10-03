package ru.astrainteractive.messagebridge.link.permission

import ru.astrainteractive.astralibs.server.permission.Permission

internal sealed class LinkPermission(override val value: String) : Permission {
    data object UnlinkPlayer : LinkPermission("tbridge.unlink.player")
}
