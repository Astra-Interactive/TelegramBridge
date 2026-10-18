package ru.astrainteractive.messagebridge.integration.luckperms.role.fake

import ru.astrainteractive.messagebridge.integration.luckperms.role.api.LinkRoleRepository
import java.util.UUID

class FakeLinkRoleRepository : LinkRoleRepository {
    val added = mutableListOf<UUID>()
    val removed = mutableListOf<UUID>()

    override fun addLinkRole(uuid: UUID) {
        added.add(uuid)
    }

    override fun removeLinkedRole(uuid: UUID) {
        removed.add(uuid)
    }
}
