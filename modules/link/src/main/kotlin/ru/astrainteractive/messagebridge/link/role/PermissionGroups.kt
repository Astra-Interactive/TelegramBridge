package ru.astrainteractive.messagebridge.link.role

import java.util.UUID

/** Groups of a Minecraft player in the permission plugin. */
internal interface PermissionGroups {
    suspend fun add(uuid: UUID, group: String): Result<Unit>

    suspend fun remove(uuid: UUID, group: String): Result<Unit>
}
