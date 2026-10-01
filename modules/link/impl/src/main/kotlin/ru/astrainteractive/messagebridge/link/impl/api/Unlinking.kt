package ru.astrainteractive.messagebridge.link.impl.api

import ru.astrainteractive.messagebridge.link.impl.model.UnlinkResponse
import java.util.UUID

internal interface Unlinking {
    suspend fun unlink(uuid: UUID): UnlinkResponse
}
