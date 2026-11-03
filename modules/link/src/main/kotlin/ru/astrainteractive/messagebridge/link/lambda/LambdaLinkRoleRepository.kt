package ru.astrainteractive.messagebridge.link.lambda

import ru.astrainteractive.messagebridge.link.api.LinkRoleRepository
import java.util.UUID

internal class LambdaLinkRoleRepository(
    private val addLinkRole: (UUID) -> Unit,
    private val removeLinkedRole: (UUID) -> Unit
) : LinkRoleRepository {
    override fun addLinkRole(uuid: UUID) {
        addLinkRole.invoke(uuid)
    }

    override fun removeLinkedRole(uuid: UUID) {
        removeLinkedRole.invoke(uuid)
    }
}
