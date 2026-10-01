package ru.astrainteractive.messagebridge.core.api.fake

import net.kyori.adventure.text.Component
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.annotation.InternalPlatformApi
import ru.astrainteractive.astralibs.server.location.KLocation
import ru.astrainteractive.astralibs.server.permission.Permission
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import java.net.InetSocketAddress
import java.util.Locale
import java.util.UUID

@OptIn(InternalPlatformApi::class)
class RecordingOnlineKPlayer(
    override val uuid: UUID,
    override val name: String,
    private val permissions: Set<Permission> = emptySet()
) : OnlineKPlayer {
    val messages = mutableListOf<LocalizableComponent>()
    val components = mutableListOf<Component>()

    override val locale: Locale = Locale.ROOT

    override val address: InetSocketAddress = InetSocketAddress(0)

    override fun hasPlayedBefore(): Boolean = true

    override fun sendMessage(component: Component) {
        components.add(component)
    }

    override fun sendMessage(message: LocalizableComponent) {
        messages.add(message)
    }

    override fun getLocation(): KLocation = error("Commands never read a location")

    override fun teleport(kLocation: KLocation) = error("Commands never teleport")

    override fun hasPermission(permission: Permission): Boolean = permission in permissions

    override fun maxPermissionSize(permission: Permission): Int? = null

    override fun minPermissionSize(permission: Permission): Int? = null

    override fun permissionSizes(permission: Permission): List<Int> = emptyList()

    override fun dispatchCommand(command: String) = Unit
}
