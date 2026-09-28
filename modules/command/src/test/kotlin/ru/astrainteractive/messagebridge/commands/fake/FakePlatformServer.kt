package ru.astrainteractive.messagebridge.commands.fake

import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.KPlayer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import java.util.UUID

internal class FakePlatformServer(
    private val onlinePlayers: List<OnlineKPlayer>
) : PlatformServer {
    override fun getOnlinePlayers(): List<OnlineKPlayer> = onlinePlayers

    override fun findOnlinePlayer(uuid: UUID): OnlineKPlayer? {
        return onlinePlayers.firstOrNull { player -> player.uuid == uuid }
    }

    override fun findOfflinePlayer(uuid: UUID): KPlayer? = findOnlinePlayer(uuid)

    override fun findOnlinePlayer(name: String): OnlineKPlayer? {
        return onlinePlayers.firstOrNull { player -> player.name == name }
    }

    override fun findOfflinePlayer(name: String): KPlayer? = findOnlinePlayer(name)
}
