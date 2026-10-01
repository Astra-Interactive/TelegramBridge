package ru.astrainteractive.messagebridge.messenger.telegram.command

import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider

internal class FakeOnlinePlayersProvider(
    private val players: List<String>
) : OnlinePlayersProvider {
    override fun provide(): List<String> = players
}
