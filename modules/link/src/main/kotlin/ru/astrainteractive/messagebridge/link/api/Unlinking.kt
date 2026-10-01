package ru.astrainteractive.messagebridge.link.api

import ru.astrainteractive.messagebridge.link.model.UnlinkResponse
import java.util.UUID

interface Unlinking {
    suspend fun unlink(uuid: UUID): UnlinkResponse
}
