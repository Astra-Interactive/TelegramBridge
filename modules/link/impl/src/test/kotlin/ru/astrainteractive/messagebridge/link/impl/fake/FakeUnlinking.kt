package ru.astrainteractive.messagebridge.link.impl.fake

import ru.astrainteractive.messagebridge.link.impl.api.Unlinking
import ru.astrainteractive.messagebridge.link.impl.model.UnlinkResponse
import java.util.UUID

internal class FakeUnlinking(
    var response: UnlinkResponse
) : Unlinking {
    val unlinkedPlayers = mutableListOf<UUID>()

    override suspend fun unlink(uuid: UUID): UnlinkResponse {
        unlinkedPlayers += uuid
        return response
    }
}
