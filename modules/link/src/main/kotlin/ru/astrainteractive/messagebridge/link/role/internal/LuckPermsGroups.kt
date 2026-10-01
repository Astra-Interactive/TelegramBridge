package ru.astrainteractive.messagebridge.link.role.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import net.luckperms.api.model.data.DataMutateResult
import net.luckperms.api.model.user.User
import net.luckperms.api.node.types.InheritanceNode
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.api.LuckPermsProvider
import ru.astrainteractive.messagebridge.link.role.api.PermissionGroups
import ru.astrainteractive.messagebridge.link.role.model.PermissionGroupError
import java.util.UUID

internal class LuckPermsGroups(
    private val luckPermsProvider: LuckPermsProvider
) : PermissionGroups,
    Logger by JUtiltLogger("MessageBridge-LuckPermsGroups") {

    private suspend fun modify(
        uuid: UUID,
        group: String,
        change: (User, InheritanceNode) -> DataMutateResult
    ): Result<Unit> {
        val luckPerms = luckPermsProvider.provide()
            ?: return Result.failure(PermissionGroupError("LuckPerms is not installed", null))
        val node = luckPerms.nodeBuilderRegistry.forInheritance().group(group).build()
        return runCatching {
            luckPerms.userManager
                .modifyUser(uuid) { user ->
                    val result = change(user, node)
                    info { "#modify group $group of $uuid: $result" }
                }
                .await()
        }
            .onFailure { error -> if (error is CancellationException) throw error }
            .fold(
                onSuccess = { _ -> Result.success(Unit) },
                onFailure = { error ->
                    Result.failure(PermissionGroupError("LuckPerms could not change the group $group of $uuid", error))
                }
            )
    }

    override suspend fun add(uuid: UUID, group: String): Result<Unit> {
        return modify(uuid, group) { user, node -> user.data().add(node) }
    }

    override suspend fun remove(uuid: UUID, group: String): Result<Unit> {
        return modify(uuid, group) { user, node -> user.data().remove(node) }
    }
}
