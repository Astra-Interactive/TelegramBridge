package ru.astrainteractive.messagebridge.link.impl.role.api

import java.util.UUID

internal interface PermissionGroups {
    suspend fun add(uuid: UUID, group: String): Result<Unit>

    suspend fun remove(uuid: UUID, group: String): Result<Unit>
}
