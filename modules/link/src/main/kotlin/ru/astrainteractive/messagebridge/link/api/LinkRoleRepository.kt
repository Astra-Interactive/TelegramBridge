package ru.astrainteractive.messagebridge.link.api

import java.util.UUID

interface LinkRoleRepository {
    fun addLinkRole(uuid: UUID)

    fun removeLinkedRole(uuid: UUID)
}
