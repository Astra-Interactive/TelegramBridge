package ru.astrainteractive.messagebridge.integration.luckperms.role.api

import java.util.UUID

interface LinkRoleRepository {
    fun addLinkRole(uuid: UUID)

    fun removeLinkedRole(uuid: UUID)
}
