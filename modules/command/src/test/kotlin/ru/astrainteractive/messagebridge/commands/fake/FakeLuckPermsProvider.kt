package ru.astrainteractive.messagebridge.commands.fake

import net.luckperms.api.LuckPerms
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider

internal class FakeLuckPermsProvider : LuckPermsProvider {
    var provideCallCount = 0
        private set

    override fun provide(): Result<LuckPerms> {
        provideCallCount++
        return Result.failure(IllegalStateException("LuckPerms is not installed"))
    }
}
