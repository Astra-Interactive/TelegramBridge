package ru.astrainteractive.messagebridge.commands.fake

import net.luckperms.api.LuckPerms
import ru.astrainteractive.messagebridge.core.api.LuckPermsProvider

internal class FakeLuckPermsProvider : LuckPermsProvider {
    var provideCallCount = 0
        private set

    override fun provide(): LuckPerms? {
        provideCallCount++
        return null
    }
}
