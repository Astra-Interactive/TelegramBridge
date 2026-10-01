package ru.astrainteractive.messagebridge.link

import ru.astrainteractive.messagebridge.link.role.PermissionGroups
import java.util.UUID

internal class FakePermissionGroups : PermissionGroups {
    private val groupsByPlayer = mutableMapOf<UUID, MutableSet<String>>()
    var failure: Throwable? = null

    fun groupsOf(uuid: UUID): Set<String> = groupsByPlayer[uuid].orEmpty()

    override suspend fun add(uuid: UUID, group: String): Result<Unit> {
        failure?.let { error -> return Result.failure(error) }
        groupsByPlayer.getOrPut(uuid) { mutableSetOf() }.add(group)
        return Result.success(Unit)
    }

    override suspend fun remove(uuid: UUID, group: String): Result<Unit> {
        failure?.let { error -> return Result.failure(error) }
        groupsByPlayer[uuid]?.remove(group)
        return Result.success(Unit)
    }
}
