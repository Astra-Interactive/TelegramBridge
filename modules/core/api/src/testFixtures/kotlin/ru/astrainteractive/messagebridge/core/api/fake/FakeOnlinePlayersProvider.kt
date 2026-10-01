package ru.astrainteractive.messagebridge.core.api.fake

import ru.astrainteractive.messagebridge.core.api.api.OnlinePlayersProvider

class FakeOnlinePlayersProvider(
    private val players: List<String>
) : OnlinePlayersProvider {
    override fun provide(): List<String> = players
}
