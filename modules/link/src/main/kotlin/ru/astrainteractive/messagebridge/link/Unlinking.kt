package ru.astrainteractive.messagebridge.link

import java.util.UUID

interface Unlinking {
    /**
     * Removes every link of the player and takes back the LuckPerms group linking gave. The group goes first, so a
     * failure leaves the link for the next try.
     */
    suspend fun unlink(uuid: UUID): UnlinkResponse
}
