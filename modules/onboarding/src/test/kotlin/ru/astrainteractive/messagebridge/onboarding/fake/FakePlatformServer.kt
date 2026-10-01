package ru.astrainteractive.messagebridge.onboarding.fake

import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.KPlayer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import java.util.UUID

/** A server without players: the /mb commands never look one up. */
internal class FakePlatformServer : PlatformServer {
    override fun getOnlinePlayers(): List<OnlineKPlayer> = emptyList()

    override fun findOnlinePlayer(uuid: UUID): OnlineKPlayer? = null

    override fun findOfflinePlayer(uuid: UUID): KPlayer? = null

    override fun findOnlinePlayer(name: String): OnlineKPlayer? = null

    override fun findOfflinePlayer(name: String): KPlayer? = null
}
