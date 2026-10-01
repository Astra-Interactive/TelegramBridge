package ru.astrainteractive.messagebridge.commands.fake

import ru.astrainteractive.messagebridge.link.UnlinkResponse
import ru.astrainteractive.messagebridge.link.Unlinking
import java.util.UUID

/** Answers every unlink with [response] and remembers whom it was asked to unlink. */
internal class FakeUnlinking(
    var response: UnlinkResponse
) : Unlinking {
    val unlinkedPlayers = mutableListOf<UUID>()

    override suspend fun unlink(uuid: UUID): UnlinkResponse {
        unlinkedPlayers += uuid
        return response
    }
}
