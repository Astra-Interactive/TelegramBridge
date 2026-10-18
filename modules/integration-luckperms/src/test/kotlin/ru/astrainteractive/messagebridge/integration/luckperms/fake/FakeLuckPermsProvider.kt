package ru.astrainteractive.messagebridge.integration.luckperms.fake

import net.luckperms.api.LuckPerms
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider

internal class FakeLuckPermsProvider(
    private val luckPerms: LuckPerms?
) : LuckPermsProvider {
    var provideCallCount = 0
        private set

    override fun provide(): Result<LuckPerms> {
        provideCallCount++
        if (luckPerms == null) {
            return Result.failure(IllegalStateException("LuckPerms is not installed"))
        }
        return Result.success(luckPerms)
    }
}
