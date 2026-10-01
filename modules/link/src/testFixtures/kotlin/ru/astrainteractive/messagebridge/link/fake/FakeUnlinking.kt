package ru.astrainteractive.messagebridge.link.fake

import ru.astrainteractive.messagebridge.link.api.Unlinking
import ru.astrainteractive.messagebridge.link.model.UnlinkResponse
import java.util.UUID

class FakeUnlinking(
    var response: UnlinkResponse
) : Unlinking {
    val unlinkedPlayers = mutableListOf<UUID>()

    override suspend fun unlink(uuid: UUID): UnlinkResponse {
        unlinkedPlayers += uuid
        return response
    }
}
